package com.Barbering.backend.services.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Barbering.backend.dto.DiscoveryDtos.DiscoveryListing;
import com.Barbering.backend.dto.DiscoveryDtos.DiscoveryResult;
import com.Barbering.backend.entities.BarberProfile;
import com.Barbering.backend.entities.BarberServiceOffering;
import com.Barbering.backend.entities.Review;
import com.Barbering.backend.entities.User;
import com.Barbering.backend.entities.WorkingHour;
import com.Barbering.backend.exception.ApiProblem;
import com.Barbering.backend.repositories.BarberProfileRepository;
import com.Barbering.backend.repositories.ReviewRepository;
import com.Barbering.backend.repositories.UserRepository;
import com.Barbering.backend.repositories.WorkingHourRepository;
import com.Barbering.backend.repositories.BarberServiceOfferingRepository;
import com.Barbering.backend.repositories.ServiceCatalogRepository;
import com.Barbering.backend.services.google.GooglePlacesDtos.FailureKind;
import com.Barbering.backend.services.google.GooglePlacesDtos.GooglePlacesException;
import com.Barbering.backend.services.google.GooglePlacesDtos.NearbySearchQuery;
import com.Barbering.backend.services.google.GooglePlacesDtos.PlaceSummary;
import com.Barbering.backend.services.google.GooglePlacesDtos.RankPreference;
import com.Barbering.backend.services.google.GooglePlacesProvider;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTest {

    @Mock private BarberProfileRepository profiles;
    @Mock private UserRepository users;
    @Mock private ReviewRepository reviews;
    @Mock private GooglePlacesProvider places;
    @Mock private BarberServiceOfferingRepository offerings;
    @Mock private WorkingHourRepository hours;
    @Mock private ServiceCatalogRepository catalog;

    private DiscoveryService service;

    @BeforeEach
    void setUp() {
        service = new DiscoveryService(profiles, users, reviews, places, offerings, hours, catalog);
    }

    // --- helpers (stub only what each case uses; strict stubs) ---

    private BarberProfile profile(long id, double lat, double lng, String googlePlaceId) {
        BarberProfile p = mock(BarberProfile.class);
        when(p.getId()).thenReturn(id);
        lenient().when(p.getUserId()).thenReturn(id);
        when(p.getLatitude()).thenReturn(lat);
        when(p.getLongitude()).thenReturn(lng);
        if (googlePlaceId != null) {
            when(p.getGooglePlaceId()).thenReturn(googlePlaceId);
        } else {
            when(p.getGooglePlaceId()).thenReturn(null);
        }
        return p;
    }

    private User user(String displayName) {
        User u = mock(User.class);
        if (displayName != null) {
            when(u.getDisplayName()).thenReturn(displayName);
        } else {
            when(u.getDisplayName()).thenReturn(null);
        }
        return u;
    }

    private Review review(Instant createdAt) {
        Review r = mock(Review.class);
        when(r.getCreatedAt()).thenReturn(createdAt);
        return r;
    }

    private PlaceSummary place(String placeId, String name, double lat, double lng) {
        return new PlaceSummary(placeId, name, "1 Main St, Amsterdam", "barber_shop",
                4.5, 12L, "OPERATIONAL", lat, lng,
                "https://maps.google.com/?cid=" + placeId, "+31612345678",
                "https://example.com", true);
    }

    // --- provider availability & failure mapping ---

    @Test
    void unavailableProviderKeepsMembersAndReportsUnavailable() {
        BarberProfile p = profile(7L, 52.0, 4.0, null);
        when(profiles.findAllWithCoordinates()).thenReturn(List.of(p));
        when(users.findById(7L)).thenReturn(Optional.empty());
        when(reviews.findByBarberProfileIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());
        when(places.isAvailable()).thenReturn(false);

        DiscoveryResult result = service.nearby(52.0, 4.0, 10.0, null, false, List.of(), false);

        assertFalse(result.googleAvailable());
        assertEquals("unavailable", result.googleStatus());
        assertEquals(1, result.listings().size());
        assertEquals(7L, result.listings().get(0).barberId());
    }

    @Test
    void providerFailureKindsMapToStatusStringsAndKeepMembers() {
        assertFailureStatus(FailureKind.TIMEOUT, "timeout");
        assertFailureStatus(FailureKind.QUOTA_OR_AUTHORIZATION, "quota_or_authorization");
        assertFailureStatus(FailureKind.BAD_RESPONSE, "bad_response");
    }

    private void assertFailureStatus(FailureKind kind, String expectedStatus) {
        // fresh mocks per case to keep strict stubbing clean
        reset(profiles, users, reviews, places);
        BarberProfile p = profile(7L, 52.0, 4.0, null);
        when(profiles.findAllWithCoordinates()).thenReturn(List.of(p));
        when(users.findById(7L)).thenReturn(Optional.empty());
        when(reviews.findByBarberProfileIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());
        when(places.isAvailable()).thenReturn(true);
        when(places.searchNearby(any())).thenThrow(new GooglePlacesException(kind, "boom"));

        DiscoveryResult result = service.nearby(52.0, 4.0, 10.0, null, false, List.of(), false);

        assertEquals(expectedStatus, result.googleStatus());
        assertEquals(1, result.listings().size());
        assertEquals(7L, result.listings().get(0).barberId());
    }

    // --- merged listing: dedupe, radius filter, sort order ---

    @Test
    void mergesMembersAndGooglePlacesDedupesClaimedIdsFiltersRadiusAndSortsByDistance() {
        BarberProfile near = profile(1L, 52.0, 4.0, "ChIJclaimed");
        BarberProfile farMember = profile(2L, 52.009, 4.0, null); // ~1 km away
        when(profiles.findAllWithCoordinates()).thenReturn(List.of(near, farMember));
        User sam = user("Sam");
        when(users.findById(1L)).thenReturn(Optional.of(sam));
        when(users.findById(2L)).thenReturn(Optional.empty());
        when(reviews.findByBarberProfileIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(reviews.findByBarberProfileIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());

        when(places.isAvailable()).thenReturn(true);
        PlaceSummary claimed = place("ChIJclaimed", "Claimed Shop", 52.0, 4.0);
        PlaceSummary other = place("ChIJother", "Other Shop", 52.004, 4.0); // ~0.45 km
        PlaceSummary farPlace = place("ChIJfar", "Far Shop", 53.0, 4.0);    // ~111 km > radius
        when(places.searchNearby(any())).thenReturn(List.of(claimed, other, farPlace));

        DiscoveryResult result = service.nearby(52.0, 4.0, 10.0, null, false, List.of(), false);

        assertTrue(result.googleAvailable());
        assertEquals("ok", result.googleStatus());
        assertEquals(3, result.listings().size());

        // sorted by distanceKm ascending: member@0km, google@~0.45km, member@~1km
        DiscoveryListing first = result.listings().get(0);
        DiscoveryListing second = result.listings().get(1);
        DiscoveryListing third = result.listings().get(2);

        assertEquals(1L, first.barberId());
        assertNull(first.placeId());
        assertEquals("Sam", first.name());
        assertEquals(0.0, first.distanceKm(), 1e-9);

        assertEquals("ChIJother", second.placeId());
        assertNull(second.barberId());
        assertEquals("Other Shop", second.name());

        assertEquals(2L, third.barberId());
        assertEquals("Barber", third.name()); // missing user falls back to "Barber"

        // the claimed place id must not appear as a google listing (deduped into member)
        for (DiscoveryListing l : result.listings()) {
            if (l.placeId() != null) {
                assertFalse("ChIJclaimed".equals(l.placeId()));
            }
        }

        verify(places).searchNearby(new NearbySearchQuery(52.0, 4.0, 20, RankPreference.DISTANCE,
                List.of("barber_shop"), null, false));
    }

    // --- member enrichment: address + lastReviewAt ---

    @Test
    void buildsMemberAddressAndLastReviewFromReviews() {
        BarberProfile p = profile(9L, 52.0, 4.0, null);
        when(p.getStreetAddress()).thenReturn("12 Main St");
        when(p.getCity()).thenReturn("Amsterdam");
        when(profiles.findAllWithCoordinates()).thenReturn(List.of(p));
        User sam = user("Sam");
        when(users.findById(9L)).thenReturn(Optional.of(sam));
        Instant stamp = Instant.parse("2026-07-01T10:00:00Z");
        Review r = review(stamp);
        when(reviews.findByBarberProfileIdOrderByCreatedAtDesc(9L))
                .thenReturn(List.of(r));
        when(places.isAvailable()).thenReturn(false);

        DiscoveryResult result = service.nearby(52.0, 4.0, 10.0, null, false, List.of(), false);

        DiscoveryListing l = result.listings().get(0);
        assertEquals("12 Main St, Amsterdam", l.address());
        assertEquals(stamp, l.lastReviewAt()); // first (most recent) review wins
    }

    @Test
    void memberWithoutAddressOrReviewsHasNulls() {
        BarberProfile p = profile(9L, 52.0, 4.0, null);
        when(p.getStreetAddress()).thenReturn(null);
        when(p.getCity()).thenReturn(null);
        when(profiles.findAllWithCoordinates()).thenReturn(List.of(p));
        when(users.findById(9L)).thenReturn(Optional.empty());
        when(reviews.findByBarberProfileIdOrderByCreatedAtDesc(9L)).thenReturn(List.of());
        when(places.isAvailable()).thenReturn(false);

        DiscoveryResult result = service.nearby(52.0, 4.0, 10.0, null, false, List.of(), false);

        DiscoveryListing l = result.listings().get(0);
        assertNull(l.address());
        assertNull(l.lastReviewAt());
    }

    // --- validation ---

    @Test
    void rejectsInvalidCoordinates() {
        ApiProblem latError = assertThrows(ApiProblem.class, () -> service.nearby(91.0, 4.0, 10.0, null, false, List.of(), false));
        assertTrue(latError.getMessage().contains("lat must be within -90..90"));

        ApiProblem lngError = assertThrows(ApiProblem.class, () -> service.nearby(52.0, -181.0, 10.0, null, false, List.of(), false));
        assertTrue(lngError.getMessage().contains("lng within -180..180"));
    }

    @Test
    void rejectsInvalidRadius() {
        ApiProblem zero = assertThrows(ApiProblem.class, () -> service.nearby(52.0, 4.0, 0.0, null, false, List.of(), false));
        assertTrue(zero.getMessage().contains("radiusKm must be between 0 and 100"));

        ApiProblem tooBig = assertThrows(ApiProblem.class, () -> service.nearby(52.0, 4.0, 101.0, null, false, List.of(), false));
        assertTrue(tooBig.getMessage().contains("radiusKm must be between 0 and 100"));
    }

    @Test
    void rejectsInvalidMinRating() {
        ApiProblem error = assertThrows(ApiProblem.class, () -> service.nearby(52.0, 4.0, 10.0, 5.5, false, List.of(), false));
        assertTrue(error.getMessage().contains("minRating must be between 0 and 5"));
    }

    // --- haversine geometry ---

    @Test
    void haversineKmMatchesKnownDistances() {
        assertEquals(111.19, DiscoveryService.haversineKm(0.0, 0.0, 1.0, 0.0), 0.5);
        assertEquals(0.0, DiscoveryService.haversineKm(52.37, 4.90, 52.37, 4.90), 1e-9);
    }

    @Test
    void hasCoordinatesRequiresBothNonNull() {
        assertTrue(DiscoveryService.hasCoordinates(52.0, 4.0));
        assertFalse(DiscoveryService.hasCoordinates(null, 4.0));
        assertFalse(DiscoveryService.hasCoordinates(52.0, null));
        assertFalse(DiscoveryService.hasCoordinates(null, null));
    }

    // --- review freshness (3/8-month boundaries) ---

    @Test
    void freshReviewWithinThreeMonthsIsFresh() {
        BarberProfile p = profile(7L, 52.0, 4.0, null);
        when(profiles.findAllWithCoordinates()).thenReturn(List.of(p));
        when(users.findById(7L)).thenReturn(Optional.empty());
        Review r = review(LocalDate.now().minusMonths(2).atStartOfDay(ZoneOffset.UTC).toInstant());
        when(reviews.findByBarberProfileIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(r));
        when(places.isAvailable()).thenReturn(false);

        DiscoveryResult result = service.nearby(52.0, 4.0, 10.0, null, false, List.of(), false);

        assertEquals("FRESH", result.listings().get(0).reviewFreshness());
    }

    @Test
    void staleReviewBetweenThreeAndEightMonthsIsStale() {
        BarberProfile p = profile(7L, 52.0, 4.0, null);
        when(profiles.findAllWithCoordinates()).thenReturn(List.of(p));
        when(users.findById(7L)).thenReturn(Optional.empty());
        Review r = review(LocalDate.now().minusMonths(5).atStartOfDay(ZoneOffset.UTC).toInstant());
        when(reviews.findByBarberProfileIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(r));
        when(places.isAvailable()).thenReturn(false);

        DiscoveryResult result = service.nearby(52.0, 4.0, 10.0, null, false, List.of(), false);

        assertEquals("STALE", result.listings().get(0).reviewFreshness());
    }

    @Test
    void outdatedReviewAtOrBeyondEightMonthsIsOutdated() {
        BarberProfile p = profile(7L, 52.0, 4.0, null);
        when(profiles.findAllWithCoordinates()).thenReturn(List.of(p));
        when(users.findById(7L)).thenReturn(Optional.empty());
        Review r = review(LocalDate.now().minusMonths(10).atStartOfDay(ZoneOffset.UTC).toInstant());
        when(reviews.findByBarberProfileIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(r));
        when(places.isAvailable()).thenReturn(false);

        DiscoveryResult result = service.nearby(52.0, 4.0, 10.0, null, false, List.of(), false);

        assertEquals("OUTDATED", result.listings().get(0).reviewFreshness());
    }

    @Test
    void memberWithNoReviewsHasNullFreshness() {
        BarberProfile p = profile(7L, 52.0, 4.0, null);
        when(profiles.findAllWithCoordinates()).thenReturn(List.of(p));
        when(users.findById(7L)).thenReturn(Optional.empty());
        when(reviews.findByBarberProfileIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());
        when(places.isAvailable()).thenReturn(false);

        DiscoveryResult result = service.nearby(52.0, 4.0, 10.0, null, false, List.of(), false);

        assertNull(result.listings().get(0).lastReviewAt());
        assertNull(result.listings().get(0).reviewFreshness());
    }

    // --- member-only filters: services + bookable ---

    @Test
    void servicesFilterKeepsOnlyMembersOfferingAllRequestedServices() {
        BarberProfile match = profile(1L, 52.0, 4.0, null);
        BarberProfile noMatch = profile(2L, 52.0, 4.0, null);
        when(profiles.findAllWithCoordinates()).thenReturn(List.of(match, noMatch));

        BarberServiceOffering offer = mock(BarberServiceOffering.class);
        when(offer.getNameOverride()).thenReturn("Haircut");
        when(offerings.findByBarberProfileIdAndEnabledTrueOrderById(1L)).thenReturn(List.of(offer));
        when(offerings.findByBarberProfileIdAndEnabledTrueOrderById(2L)).thenReturn(List.of());

        User sam = user("Sam");
        when(users.findById(1L)).thenReturn(Optional.of(sam));
        when(reviews.findByBarberProfileIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        when(places.isAvailable()).thenReturn(false);

        DiscoveryResult result = service.nearby(52.0, 4.0, 10.0, null, false, List.of("haircut"), false);

        assertEquals(1, result.listings().size());
        assertEquals(1L, result.listings().get(0).barberId());
        verify(users, never()).findById(2L);
        verify(reviews, never()).findByBarberProfileIdOrderByCreatedAtDesc(2L);
    }

    @Test
    void bookableFilterKeepsOnlyMembersWithEnabledWorkingHours() {
        BarberProfile open = profile(1L, 52.0, 4.0, null);
        BarberProfile closed = profile(2L, 52.0, 4.0, null);
        when(profiles.findAllWithCoordinates()).thenReturn(List.of(open, closed));

        WorkingHour enabledHour = mock(WorkingHour.class);
        when(enabledHour.isEnabled()).thenReturn(true);
        when(hours.findByBarberProfileIdOrderByDayOfWeek(1L)).thenReturn(List.of(enabledHour));

        WorkingHour disabledHour = mock(WorkingHour.class);
        when(disabledHour.isEnabled()).thenReturn(false);
        when(hours.findByBarberProfileIdOrderByDayOfWeek(2L)).thenReturn(List.of(disabledHour));

        User sam = user("Sam");
        when(users.findById(1L)).thenReturn(Optional.of(sam));
        when(reviews.findByBarberProfileIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        when(places.isAvailable()).thenReturn(false);

        DiscoveryResult result = service.nearby(52.0, 4.0, 10.0, null, false, List.of(), true);

        assertEquals(1, result.listings().size());
        assertEquals(1L, result.listings().get(0).barberId());
        verify(users, never()).findById(2L);
    }

    @Test
    void googleListingsSkippedWhenMemberOnlyFiltersAreActive() {
        BarberProfile p = profile(1L, 52.0, 4.0, null);
        when(profiles.findAllWithCoordinates()).thenReturn(List.of(p));

        BarberServiceOffering offer = mock(BarberServiceOffering.class);
        when(offer.getNameOverride()).thenReturn("Haircut");
        when(offerings.findByBarberProfileIdAndEnabledTrueOrderById(1L)).thenReturn(List.of(offer));

        User sam = user("Sam");
        when(users.findById(1L)).thenReturn(Optional.of(sam));
        when(reviews.findByBarberProfileIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        when(places.isAvailable()).thenReturn(true);

        DiscoveryResult result = service.nearby(52.0, 4.0, 10.0, null, false, List.of("haircut"), false);

        assertTrue(result.googleAvailable());
        assertEquals("ok", result.googleStatus());
        assertNull(result.googleAttribution());
        verify(places, never()).searchNearby(any());
        for (DiscoveryListing l : result.listings()) {
            assertEquals("MEMBER", l.source());
        }
    }
}
