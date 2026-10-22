package com.Barbering.backend.services.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Barbering.backend.entities.BarberProfile;
import com.Barbering.backend.entities.PlaceClaim;
import com.Barbering.backend.exception.ApiProblem;
import com.Barbering.backend.repositories.BarberProfileRepository;
import com.Barbering.backend.repositories.PlaceClaimRepository;
import com.Barbering.backend.dto.DiscoveryDtos.PlaceClaimDto;
import com.Barbering.backend.services.google.GooglePlacesDtos.FailureKind;
import com.Barbering.backend.services.google.GooglePlacesDtos.GooglePlacesException;
import com.Barbering.backend.services.google.GooglePlacesDtos.PlaceDetails;
import com.Barbering.backend.services.google.GooglePlacesProvider;

@ExtendWith(MockitoExtension.class)
class PlaceClaimServiceTest {

    @Mock private PlaceClaimRepository claims;
    @Mock private BarberProfileRepository profiles;
    @Mock private GooglePlacesProvider places;

    private PlaceClaimService service() {
        return new PlaceClaimService(claims, profiles, places);
    }

    private BarberProfile profile(long id) {
        BarberProfile p = mock(BarberProfile.class);
        when(p.getId()).thenReturn(id);
        return p;
    }

    @Test
    void requestCreatesPendingWhenNoExistingClaim() {
        BarberProfile prof = profile(7L);
        when(profiles.findByUserId(5L)).thenReturn(Optional.of(prof));
        when(claims.findByGooglePlaceId("ChIJok")).thenReturn(Optional.empty());
        when(claims.findByBarberProfileIdAndStatusIn(anyLong(), any())).thenReturn(List.of());
        when(claims.save(any(PlaceClaim.class))).thenAnswer(inv -> inv.getArgument(0));

        PlaceClaimDto dto = service().request(5L, "ChIJok");

        assertEquals("PENDING", dto.status());
        assertEquals("ChIJok", dto.googlePlaceId());
        assertNotNull(dto.requestedAt());

        ArgumentCaptor<PlaceClaim> cap = ArgumentCaptor.forClass(PlaceClaim.class);
        verify(claims).save(cap.capture());
        PlaceClaim saved = cap.getValue();
        assertEquals(5L, saved.getUserId());
        assertEquals(7L, saved.getBarberProfileId());
        assertEquals("ChIJok", saved.getGooglePlaceId());
    }

    @Test
    void requestRejectsInvalidPlaceIds() {
        when(profiles.findByUserId(5L)).thenReturn(Optional.of(mock(BarberProfile.class)));
        for (String bad : new String[] {"bad id", "", "a".repeat(65), null}) {
            ApiProblem ex = assertThrows(ApiProblem.class, () -> service().request(5L, bad));
            assertTrue(ex.getMessage().contains("googlePlaceId must match"));
        }
    }

    @Test
    void requestRequiresBarberProfile() {
        when(profiles.findByUserId(5L)).thenReturn(Optional.empty());
        ApiProblem ex = assertThrows(ApiProblem.class, () -> service().request(5L, "ChIJok"));
        assertTrue(ex.getMessage().contains("Barber profile not found"));
    }

    @Test
    void requestConflictsWhenPlaceAlreadyClaimed() {
        when(profiles.findByUserId(5L)).thenReturn(Optional.of(mock(BarberProfile.class)));
        PlaceClaim existing = new PlaceClaim(9L, 8L, "ChIJtaken"); // PENDING by ctor
        when(claims.findByGooglePlaceId("ChIJtaken")).thenReturn(Optional.of(existing));

        ApiProblem ex = assertThrows(ApiProblem.class, () -> service().request(5L, "ChIJtaken"));
        assertTrue(ex.getMessage().contains("A claim for this place is already PENDING"));
    }

    @Test
    void requestReopensRejectedClaimWithoutSaving() {
        when(profiles.findByUserId(5L)).thenReturn(Optional.of(mock(BarberProfile.class)));
        PlaceClaim rejected = new PlaceClaim(9L, 8L, "ChIJold");
        rejected.setStatus(PlaceClaim.ClaimStatus.REJECTED);
        rejected.setReviewedAt(Instant.parse("2026-01-05T10:00:00Z"));
        when(claims.findByGooglePlaceId("ChIJold")).thenReturn(Optional.of(rejected));

        PlaceClaimDto dto = service().request(5L, "ChIJold");

        assertEquals("PENDING", dto.status());
        assertNull(dto.reviewedAt());
        verify(claims, never()).save(any(PlaceClaim.class));
    }

    @Test
    void requestConflictsWhenUserHasActiveClaimElsewhere() {
        BarberProfile prof = profile(7L);
        when(profiles.findByUserId(5L)).thenReturn(Optional.of(prof));
        when(claims.findByGooglePlaceId("ChIJnew")).thenReturn(Optional.empty());
        PlaceClaim other = new PlaceClaim(5L, 7L, "ChIJother"); // active (PENDING) on another place
        when(claims.findByBarberProfileIdAndStatusIn(anyLong(), any())).thenReturn(List.of(other));

        ApiProblem ex = assertThrows(ApiProblem.class, () -> service().request(5L, "ChIJnew"));
        assertTrue(ex.getMessage().contains("You already have an active claim for another place"));
    }

    @Test
    void currentReturnsNotFoundWhenNoClaims() {
        when(profiles.findByUserId(5L)).thenReturn(Optional.of(mock(BarberProfile.class)));
        when(claims.findByBarberProfileId(anyLong())).thenReturn(List.of());
        ApiProblem ex = assertThrows(ApiProblem.class, () -> service().current(5L));
        assertTrue(ex.getMessage().contains("No Google place claim found"));
    }

    @Test
    void currentReturnsLatestRequestedClaim() {
        when(profiles.findByUserId(5L)).thenReturn(Optional.of(mock(BarberProfile.class)));
        PlaceClaim older = new PlaceClaim(5L, 7L, "ChIJold");
        older.setRequestedAt(Instant.parse("2026-01-01T00:00:00Z"));
        PlaceClaim newer = new PlaceClaim(5L, 7L, "ChIJnew");
        newer.setRequestedAt(Instant.parse("2026-02-01T00:00:00Z"));
        when(claims.findByBarberProfileId(anyLong())).thenReturn(List.of(older, newer));

        PlaceClaimDto dto = service().current(5L);
        assertEquals("ChIJnew", dto.googlePlaceId());
    }

    @Test
    void approveRequiresExistingClaim() {
        when(claims.findById(anyLong())).thenReturn(Optional.empty());
        ApiProblem ex = assertThrows(ApiProblem.class, () -> service().approve(5L, 100L));
        assertTrue(ex.getMessage().contains("Place claim not found"));
    }

    @Test
    void approveRejectsForeignClaim() {
        PlaceClaim foreign = new PlaceClaim(9L, 8L, "ChIJforeign"); // owned by user 9
        when(claims.findById(100L)).thenReturn(Optional.of(foreign));
        ApiProblem ex = assertThrows(ApiProblem.class, () -> service().approve(5L, 100L));
        assertTrue(ex.getMessage().contains("This claim belongs to another account"));
    }

    @Test
    void approveRequiresPendingStatus() {
        PlaceClaim approved = new PlaceClaim(5L, 7L, "ChIJdone");
        approved.setStatus(PlaceClaim.ClaimStatus.APPROVED);
        when(claims.findById(100L)).thenReturn(Optional.of(approved));
        ApiProblem ex = assertThrows(ApiProblem.class, () -> service().approve(5L, 100L));
        assertTrue(ex.getMessage().contains("Only pending claims can be approved"));
    }

    @Test
    void approveSuccessUpdatesProfileAndClaim() {
        PlaceClaim claim = new PlaceClaim(5L, 7L, "ChIJok"); // PENDING by ctor
        when(claims.findById(100L)).thenReturn(Optional.of(claim));
        when(places.fetchDetails("ChIJok")).thenReturn(new PlaceDetails(
                "ChIJok", "Ok Shop", "9 Ok St, Amsterdam", 4.8, 30L,
                "OPERATIONAL", 52.1, 4.1, "https://maps.google.com/?cid=ChIJok",
                "+31612345678", "https://example.com", true,
                OffsetDateTime.parse("2026-07-01T12:00:00Z")));
        BarberProfile prof = mock(BarberProfile.class);
        when(profiles.findById(7L)).thenReturn(Optional.of(prof));

        PlaceClaimDto dto = service().approve(5L, 100L);

        assertEquals("APPROVED", dto.status());
        assertNotNull(dto.reviewedAt());
        verify(prof).setGooglePlaceId("ChIJok");
        verify(prof).setLatitude(52.1);
        verify(prof).setLongitude(4.1);
        verify(profiles).save(prof);
    }

    @Test
    void approveSurfacesProviderFailure() {
        PlaceClaim claim = new PlaceClaim(5L, 7L, "ChIJok"); // PENDING by ctor
        when(claims.findById(100L)).thenReturn(Optional.of(claim));
        when(places.fetchDetails("ChIJok"))
                .thenThrow(new GooglePlacesException(FailureKind.QUOTA_OR_AUTHORIZATION, "quota exceeded"));

        ApiProblem ex = assertThrows(ApiProblem.class, () -> service().approve(5L, 100L));
        assertTrue(ex.getMessage().contains("Could not verify the Google place"));
        assertTrue(ex.getMessage().contains("quota exceeded"));
    }

    @Test
    void rejectPendingClaim() {
        PlaceClaim claim = new PlaceClaim(5L, 7L, "ChIJok"); // PENDING by ctor
        when(claims.findById(100L)).thenReturn(Optional.of(claim));

        PlaceClaimDto dto = service().reject(5L, 100L);

        assertEquals("REJECTED", dto.status());
        assertNotNull(dto.reviewedAt());
    }

    @Test
    void rejectRequiresPendingStatus() {
        PlaceClaim rejected = new PlaceClaim(5L, 7L, "ChIJok");
        rejected.setStatus(PlaceClaim.ClaimStatus.REJECTED);
        when(claims.findById(100L)).thenReturn(Optional.of(rejected));
        ApiProblem ex = assertThrows(ApiProblem.class, () -> service().reject(5L, 100L));
        assertTrue(ex.getMessage().contains("Only pending claims can be rejected"));
    }
}
