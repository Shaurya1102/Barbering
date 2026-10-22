package com.Barbering.backend.dto;

import java.time.Instant;
import java.util.List;

/**
 * Public discovery contract: merged listings from member profiles and Google
 * Places (New) results around a point. Ratings/review counts are only present
 * when the source actually provides them — null means "no reliable value".
 */
public final class DiscoveryDtos {

    private DiscoveryDtos() {
    }

    /**
     * One nearby listing. {@code source} is "MEMBER" (a Barbering profile with
     * coordinates) or "GOOGLE" (an external Google Maps listing). Ratings and
     * review counts are only present when the source actually provides them —
     * null means "no reliable value". {@code reviewFreshness} is derived from a
     * real last-review timestamp at 3/8-month boundaries; null = no review data.
     */
    public record DiscoveryListing(
            String source,
            Long barberId,
            String placeId,
            String name,
            String address,
            double distanceKm,
            Double rating,
            Long reviewCount,
            Boolean openNow,
            String websiteUri,
            String phone,
            String mapsUri,
            String directionsUri,
            Instant lastReviewAt,
            String reviewFreshness) {

        public static DiscoveryListing member(Long barberId, String name, String address, double distanceKm, Instant lastReviewAt) {
            return new DiscoveryListing("MEMBER", barberId, null, name, address, distanceKm, null, null, null, null, null, null, null, lastReviewAt, null);
        }

        public static DiscoveryListing member(
                Long barberId,
                String name,
                String address,
                double distanceKm,
                Instant lastReviewAt,
                String directionsUri,
                String reviewFreshness) {
            return new DiscoveryListing("MEMBER", barberId, null, name, address, distanceKm, null, null, null, null, null, null, directionsUri, lastReviewAt, reviewFreshness);
        }

        public static DiscoveryListing google(
                String placeId,
                String name,
                String address,
                double distanceKm,
                Double rating,
                Long reviewCount,
                Boolean openNow,
                String websiteUri,
                String phone,
                String mapsUri) {
            return new DiscoveryListing("GOOGLE", null, placeId, name, address, distanceKm, rating, reviewCount, openNow, websiteUri, phone, mapsUri, null, null, null);
        }

        public static DiscoveryListing google(
                String placeId,
                String name,
                String address,
                double distanceKm,
                Double rating,
                Long reviewCount,
                Boolean openNow,
                String websiteUri,
                String phone,
                String mapsUri,
                String directionsUri) {
            return new DiscoveryListing("GOOGLE", null, placeId, name, address, distanceKm, rating, reviewCount, openNow, websiteUri, phone, mapsUri, directionsUri, null, null);
        }
    }

    public record DiscoveryResult(List<DiscoveryListing> listings, boolean googleAvailable, String googleStatus, String googleAttribution) {

        /** Backwards-compatible constructor for callers/tests that predate attribution. */
        public DiscoveryResult(List<DiscoveryListing> listings, boolean googleAvailable, String googleStatus) {
            this(listings, googleAvailable, googleStatus, null);
        }
    }

    /** Claim lifecycle view for the owning barber. */
    public record PlaceClaimDto(Long id, String googlePlaceId, String status, Instant requestedAt, Instant reviewedAt) {
    }
}
