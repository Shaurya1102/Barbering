package com.Barbering.backend.services.google;

import java.util.List;

/** Replaceable boundary for a configured Google Places (New) provider. */
public interface GooglePlacesProvider {
    /** True when a server-side API key is configured and the provider can serve requests. */
    boolean isAvailable();

    List<GooglePlacesDtos.PlaceSummary> searchNearby(GooglePlacesDtos.NearbySearchQuery query);

    GooglePlacesDtos.PlaceDetails fetchDetails(String placeId);
}
