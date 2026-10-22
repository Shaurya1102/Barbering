package com.Barbering.backend.services.google;

import java.time.OffsetDateTime;
import java.util.List;

/** Provider-facing data contracts for the Google Places (New) boundary. */
public final class GooglePlacesDtos {
    private GooglePlacesDtos() {}

    public enum RankPreference { DISTANCE, RELEVANCE }

    /** Nearby search request; maxResultCount is clamped to 1..20 by the provider. */
    public record NearbySearchQuery(
            double latitude,
            double longitude,
            int maxResultCount,
            RankPreference rankPreference,
            List<String> includedTypes,
            Double minRating,
            Boolean openNow) {

        public static NearbySearchQuery barberNearby(double lat, double lng, int maxResults, Double minRating, Boolean openNow) {
            return new NearbySearchQuery(
                    lat, lng, Math.min(Math.max(maxResults, 1), 20), RankPreference.DISTANCE,
                    List.of("barber_shop"), minRating, openNow);
        }
    }

    /** One place from a nearby search. Nullable fields mean the provider did not return them. */
    public record PlaceSummary(
            String placeId,
            String name,
            String formattedAddress,
            String primaryType,
            Double rating,
            Long userRatingCount,
            String businessStatus,
            double latitude,
            double longitude,
            String googleMapsUri,
            String nationalPhoneNumber,
            String websiteUri,
            Boolean openNow) {
    }

    /** Fresh place details fetched for claim verification. lastReviewAt is only set when the provider actually returns a review date. */
    public record PlaceDetails(
            String placeId,
            String name,
            String formattedAddress,
            Double rating,
            Long userRatingCount,
            String businessStatus,
            double latitude,
            double longitude,
            String googleMapsUri,
            String nationalPhoneNumber,
            String websiteUri,
            Boolean openNow,
            OffsetDateTime lastReviewAt) {
    }

    public enum FailureKind { UNAVAILABLE, TIMEOUT, QUOTA_OR_AUTHORIZATION, BAD_RESPONSE }

    /** Provider-level failure; the controller maps kinds to honest user-facing states. */
    public static class GooglePlacesException extends RuntimeException {
        private final FailureKind kind;

        public GooglePlacesException(FailureKind kind, String message) {
            super(message);
            this.kind = kind;
        }

        public FailureKind kind() {
            return kind;
        }
    }
}
