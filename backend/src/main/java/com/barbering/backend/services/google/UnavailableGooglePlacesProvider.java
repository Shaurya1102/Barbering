package com.Barbering.backend.services.google;

import com.Barbering.backend.services.google.GooglePlacesDtos.FailureKind;
import com.Barbering.backend.services.google.GooglePlacesDtos.GooglePlacesException;
import com.Barbering.backend.services.google.GooglePlacesDtos.NearbySearchQuery;
import com.Barbering.backend.services.google.GooglePlacesDtos.PlaceDetails;
import com.Barbering.backend.services.google.GooglePlacesDtos.PlaceSummary;

import java.util.List;

/** Honest fallback used when no server-side Google Places API key is configured. */
public class UnavailableGooglePlacesProvider implements GooglePlacesProvider {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public List<PlaceSummary> searchNearby(NearbySearchQuery query) {
        throw unavailable();
    }

    @Override
    public PlaceDetails fetchDetails(String placeId) {
        throw unavailable();
    }

    private GooglePlacesException unavailable() {
        return new GooglePlacesException(FailureKind.UNAVAILABLE,
                "Google Places is not configured. Set BARBERING_GOOGLE_PLACES_API_KEY to enable nearby discovery.");
    }
}
