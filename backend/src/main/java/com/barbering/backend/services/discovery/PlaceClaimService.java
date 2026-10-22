package com.Barbering.backend.services.discovery;

import com.Barbering.backend.dto.DiscoveryDtos.PlaceClaimDto;
import com.Barbering.backend.entities.BarberProfile;
import com.Barbering.backend.entities.PlaceClaim;
import com.Barbering.backend.entities.PlaceClaim.ClaimStatus;
import com.Barbering.backend.exception.ApiProblem;
import com.Barbering.backend.repositories.BarberProfileRepository;
import com.Barbering.backend.repositories.PlaceClaimRepository;
import com.Barbering.backend.services.google.GooglePlacesDtos.GooglePlacesException;
import com.Barbering.backend.services.google.GooglePlacesProvider;
import com.Barbering.backend.services.google.GooglePlacesDtos.PlaceDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Owner-attested Google place claims. Only the profile owner may request,
 * approve, or reject a claim. Approval re-fetches live place details and
 * copies coordinates + google_place_id onto the member profile (no other
 * place data is persisted, per Places ToS).
 */
@Service
@Transactional
public class PlaceClaimService {

    private static final Pattern PLACE_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final PlaceClaimRepository claims;
    private final BarberProfileRepository profiles;
    private final GooglePlacesProvider places;

    public PlaceClaimService(PlaceClaimRepository claims, BarberProfileRepository profiles, GooglePlacesProvider places) {
        this.claims = claims;
        this.profiles = profiles;
        this.places = places;
    }

    @Transactional
    public PlaceClaimDto request(Long userId, String googlePlaceId) {
        BarberProfile profile = requireProfile(userId);
        if (googlePlaceId == null || !PLACE_ID.matcher(googlePlaceId).matches()) {
            throw ApiProblem.badRequest("googlePlaceId must match [A-Za-z0-9_-]{1,64}");
        }

        PlaceClaim existingForPlace = claims.findByGooglePlaceId(googlePlaceId).orElse(null);
        if (existingForPlace != null) {
            if (existingForPlace.getStatus() == ClaimStatus.PENDING || existingForPlace.getStatus() == ClaimStatus.APPROVED) {
                throw ApiProblem.conflict("A claim for this place is already " + existingForPlace.getStatus());
            }
            // Previously rejected: allow the owner to re-request.
            existingForPlace.setStatus(ClaimStatus.PENDING);
            existingForPlace.setRequestedAt(Instant.now());
            existingForPlace.setReviewedAt(null);
            return toDto(existingForPlace);
        }

        List<PlaceClaim> active = claims.findByBarberProfileIdAndStatusIn(profile.getId(), List.of(ClaimStatus.PENDING, ClaimStatus.APPROVED));
        if (!active.isEmpty()) {
            throw ApiProblem.conflict("You already have an active claim for another place");
        }

        PlaceClaim created = new PlaceClaim(userId, profile.getId(), googlePlaceId);
        return toDto(claims.save(created));
    }

    @Transactional(readOnly = true)
    public PlaceClaimDto current(Long userId) {
        BarberProfile profile = requireProfile(userId);
        List<PlaceClaim> all = claims.findByBarberProfileId(profile.getId());
        if (all.isEmpty()) {
            throw ApiProblem.notFound("No Google place claim found");
        }
        return toDto(all.stream().max(Comparator.comparing(PlaceClaim::getRequestedAt, Comparator.nullsFirst(Comparator.naturalOrder()))).orElseThrow());
    }

    @Transactional
    public PlaceClaimDto approve(Long userId, Long claimId) {
        PlaceClaim claim = requireOwned(userId, claimId);
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw ApiProblem.conflict("Only pending claims can be approved");
        }

        PlaceDetails details;
        try {
            details = places.fetchDetails(claim.getGooglePlaceId());
        } catch (GooglePlacesException e) {
            throw ApiProblem.unavailable("Could not verify the Google place: " + e.getMessage());
        }

        BarberProfile profile = profiles.findById(claim.getBarberProfileId()).orElseThrow(() -> ApiProblem.notFound("Barber profile not found"));
        profile.setGooglePlaceId(details.placeId());
        profile.setLatitude(details.latitude());
        profile.setLongitude(details.longitude());
        profiles.save(profile);

        claim.setStatus(ClaimStatus.APPROVED);
        claim.setReviewedAt(Instant.now());
        claims.save(claim);
        return toDto(claim);
    }

    @Transactional
    public PlaceClaimDto reject(Long userId, Long claimId) {
        PlaceClaim claim = requireOwned(userId, claimId);
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw ApiProblem.conflict("Only pending claims can be rejected");
        }
        claim.setStatus(ClaimStatus.REJECTED);
        claim.setReviewedAt(Instant.now());
        claims.save(claim);
        return toDto(claim);
    }

    private BarberProfile requireProfile(Long userId) {
        return profiles.findByUserId(userId).orElseThrow(() -> ApiProblem.notFound("Barber profile not found"));
    }

    private PlaceClaim requireOwned(Long userId, Long claimId) {
        PlaceClaim claim = claims.findById(claimId).orElseThrow(() -> ApiProblem.notFound("Place claim not found"));
        if (!userId.equals(claim.getUserId())) {
            throw ApiProblem.forbidden("This claim belongs to another account");
        }
        return claim;
    }

    private static PlaceClaimDto toDto(PlaceClaim claim) {
        return new PlaceClaimDto(claim.getId(), claim.getGooglePlaceId(), claim.getStatus().name(), claim.getRequestedAt(), claim.getReviewedAt());
    }
}
