package com.Barbering.backend.services.discovery;

import com.Barbering.backend.dto.DiscoveryDtos.DiscoveryListing;
import com.Barbering.backend.dto.DiscoveryDtos.DiscoveryResult;
import com.Barbering.backend.entities.BarberProfile;
import com.Barbering.backend.entities.BarberServiceOffering;
import com.Barbering.backend.entities.Review;
import com.Barbering.backend.entities.ServiceCatalogItem;
import com.Barbering.backend.entities.User;
import com.Barbering.backend.entities.WorkingHour;
import com.Barbering.backend.exception.ApiProblem;
import com.Barbering.backend.repositories.BarberProfileRepository;
import com.Barbering.backend.repositories.BarberServiceOfferingRepository;
import com.Barbering.backend.repositories.ReviewRepository;
import com.Barbering.backend.repositories.ServiceCatalogRepository;
import com.Barbering.backend.repositories.UserRepository;
import com.Barbering.backend.repositories.WorkingHourRepository;
import com.Barbering.backend.services.google.GooglePlacesDtos.GooglePlacesException;
import com.Barbering.backend.services.google.GooglePlacesProvider;
import com.Barbering.backend.services.google.GooglePlacesDtos.NearbySearchQuery;
import com.Barbering.backend.services.google.GooglePlacesDtos.PlaceSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Public "nearby barbers" discovery: merges member profiles that carry
 * coordinates with live Google Places (New) results around the same point.
 * Member listings are always returned even when the Google provider fails —
 * the response then reports googleAvailable=false and a failure status.
 */
@Service
@Transactional(readOnly = true)
public class DiscoveryService {

    private static final double MAX_RADIUS_KM = 100.0;
    private static final int GOOGLE_MAX_RESULTS = 20;
    /** Attribution string required when Google Places data is shown to users. */
    private static final String GOOGLE_ATTRIBUTION = "Data from Google Maps";

    private final BarberProfileRepository profiles;
    private final UserRepository users;
    private final ReviewRepository reviews;
    private final GooglePlacesProvider places;
    private final BarberServiceOfferingRepository offerings;
    private final WorkingHourRepository hours;
    private final ServiceCatalogRepository catalog;

    public DiscoveryService(BarberProfileRepository profiles, UserRepository users, ReviewRepository reviews, GooglePlacesProvider places,
                            BarberServiceOfferingRepository offerings, WorkingHourRepository hours, ServiceCatalogRepository catalog) {
        this.profiles = profiles;
        this.users = users;
        this.reviews = reviews;
        this.places = places;
        this.offerings = offerings;
        this.hours = hours;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public DiscoveryResult nearby(double lat, double lng, double radiusKm, Double minRating, boolean openNow, List<String> services, boolean bookable) {
        if (services == null) services = List.of();
        if (Math.abs(lat) > 90.0 || Math.abs(lng) > 180.0) {
            throw ApiProblem.badRequest("lat must be within -90..90 and lng within -180..180");
        }
        if (radiusKm <= 0.0 || radiusKm > MAX_RADIUS_KM) {
            throw ApiProblem.badRequest("radiusKm must be between 0 and " + MAX_RADIUS_KM);
        }
        if (minRating != null && (minRating < 0.0 || minRating > 5.0)) {
            throw ApiProblem.badRequest("minRating must be between 0 and 5");
        }

        List<BarberProfile> memberProfiles = profiles.findAllWithCoordinates();
        Set<String> claimedPlaceIds = new HashSet<>();
        for (BarberProfile profile : memberProfiles) {
            if (profile.getGooglePlaceId() != null && !profile.getGooglePlaceId().isBlank()) {
                claimedPlaceIds.add(profile.getGooglePlaceId());
            }
        }

        final Instant now = Instant.now();
        List<DiscoveryListing> listings = new ArrayList<>();
        for (BarberProfile profile : memberProfiles) {
            double distanceKm = haversineKm(lat, lng, profile.getLatitude(), profile.getLongitude());
            if (distanceKm > radiusKm) {
                continue;
            }
            if (!memberMatchesFilters(profile, services, bookable)) {
                continue;
            }
            User user = users.findById(profile.getUserId()).orElse(null);
            String name = user != null && user.getDisplayName() != null && !user.getDisplayName().isBlank()
                    ? user.getDisplayName()
                    : "Barber";
            List<Review> profileReviews = reviews.findByBarberProfileIdOrderByCreatedAtDesc(profile.getId());
            Instant lastReviewAt = profileReviews.isEmpty() ? null : profileReviews.get(0).getCreatedAt();
            String memberDirectionsUri = hasCoordinates(profile.getLatitude(), profile.getLongitude())
                    ? directionsUri(profile.getLatitude(), profile.getLongitude())
                    : null;
            listings.add(DiscoveryListing.member(
                    profile.getId(), name, addressOf(profile), distanceKm, lastReviewAt,
                    memberDirectionsUri, reviewFreshness(lastReviewAt, now)));
        }

        boolean googleAvailable = places.isAvailable();
        String googleStatus = "ok";
        boolean googleIncluded = false;
        // Member-only filters (services / bookable) cannot be verified against Google
        // PlaceSummary data, so when active we skip Google listings entirely.
        boolean memberFiltersActive = !services.isEmpty() || bookable;
        if (!googleAvailable) {
            googleStatus = "unavailable";
        } else if (memberFiltersActive) {
            googleStatus = "ok";
        } else {
            try {
                List<PlaceSummary> found = places.searchNearby(NearbySearchQuery.barberNearby(lat, lng, GOOGLE_MAX_RESULTS, minRating, openNow));
                for (PlaceSummary place : found) {
                    double distanceKm = haversineKm(lat, lng, place.latitude(), place.longitude());
                    if (distanceKm > radiusKm) {
                        continue;
                    }
                    // A member profile that already claimed this Google place is listed as MEMBER.
                    if (claimedPlaceIds.contains(place.placeId())) {
                        continue;
                    }
                    googleIncluded = true;
                    listings.add(DiscoveryListing.google(
                            place.placeId(),
                            place.name(),
                            place.formattedAddress(),
                            distanceKm,
                            place.rating(),
                            place.userRatingCount(),
                            place.openNow(),
                            place.websiteUri(),
                            place.nationalPhoneNumber(),
                            place.googleMapsUri(),
                            directionsUri(place.latitude(), place.longitude())));
                }
            } catch (GooglePlacesException e) {
                googleStatus = statusOf(e);
            }
        }

        listings.sort(Comparator.comparingDouble(DiscoveryListing::distanceKm));
        String googleAttribution = (googleAvailable && googleIncluded) ? GOOGLE_ATTRIBUTION : null;
        return new DiscoveryResult(listings, googleAvailable, googleStatus, googleAttribution);
    }

    private static String addressOf(BarberProfile profile) {
        List<String> parts = new ArrayList<>();
        if (profile.getStreetAddress() != null && !profile.getStreetAddress().isBlank()) {
            parts.add(profile.getStreetAddress());
        }
        if (profile.getCity() != null && !profile.getCity().isBlank()) {
            parts.add(profile.getCity());
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    /**
     * Member-only filter check. When a services list is supplied, the member must offer
     * every requested service (effective name = override if set, else catalog name).
     * When bookable is required, the member must have at least one enabled working hour.
     */
    private boolean memberMatchesFilters(BarberProfile profile, List<String> services, boolean bookable) {
        if (!services.isEmpty()) {
            Set<String> wanted = new HashSet<>();
            for (String s : services) {
                if (s != null && !s.isBlank()) {
                    wanted.add(s.trim().toLowerCase());
                }
            }
            if (!wanted.isEmpty()) {
                Set<String> offered = new HashSet<>();
                for (BarberServiceOffering offering : offerings.findByBarberProfileIdAndEnabledTrueOrderById(profile.getId())) {
                    String effectiveName = offering.getNameOverride() != null && !offering.getNameOverride().isBlank()
                            ? offering.getNameOverride()
                            : catalog.findById(offering.getServiceId()).map(ServiceCatalogItem::getName).orElse(null);
                    if (effectiveName != null) {
                        offered.add(effectiveName.trim().toLowerCase());
                    }
                }
                for (String w : wanted) {
                    if (!offered.contains(w)) {
                        return false;
                    }
                }
            }
        }
        if (bookable) {
            boolean hasEnabledHour = hours.findByBarberProfileIdOrderByDayOfWeek(profile.getId()).stream()
                    .anyMatch(WorkingHour::isEnabled);
            if (!hasEnabledHour) {
                return false;
            }
        }
        return true;
    }

    private static String statusOf(GooglePlacesException e) {
        return switch (e.kind()) {
            case UNAVAILABLE -> "unavailable";
            case TIMEOUT -> "timeout";
            case QUOTA_OR_AUTHORIZATION -> "quota_or_authorization";
            case BAD_RESPONSE -> "bad_response";
        };
    }

    /**
     * Review-freshness bucket from a real last-review timestamp at 3/8-month
     * boundaries. Returns null when there is no review data (never fabricated).
     */
    static String reviewFreshness(Instant lastReviewAt, Instant now) {
        if (lastReviewAt == null || now == null) {
            return null;
        }
        if (!lastReviewAt.isBefore(now)) {
            return "FRESH";
        }
        Period p = Period.between(lastReviewAt.atZone(ZoneOffset.UTC).toLocalDate(), now.atZone(ZoneOffset.UTC).toLocalDate());
        int months = p.getYears() * 12 + p.getMonths();
        if (months < 3) {
            return "FRESH";
        }
        if (months < 8) {
            return "STALE";
        }
        return "OUTDATED";
    }

    /** Google Maps directions link to a coordinate. */
    static String directionsUri(double lat, double lng) {
        return "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng;
    }

    /** Great-circle distance in kilometres. */
    static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    static boolean hasCoordinates(Double lat, Double lng) {
        return Objects.nonNull(lat) && Objects.nonNull(lng);
    }
}
