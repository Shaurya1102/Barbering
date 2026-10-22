package com.Barbering.backend.services.google;

import com.Barbering.backend.services.google.GooglePlacesDtos.FailureKind;
import com.Barbering.backend.services.google.GooglePlacesDtos.GooglePlacesException;
import com.Barbering.backend.services.google.GooglePlacesDtos.NearbySearchQuery;
import com.Barbering.backend.services.google.GooglePlacesDtos.PlaceDetails;
import com.Barbering.backend.services.google.GooglePlacesDtos.PlaceSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Server-side Google Places API (New) client. The API key stays on the server and is
 * only ever sent in the X-Goog-Api-Key header; it is never logged or returned to clients.
 */
public class HttpGooglePlacesProvider implements GooglePlacesProvider {

    private static final Pattern PLACE_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String baseUrl;
    private final Duration timeout;

    public HttpGooglePlacesProvider(String apiKey, String baseUrl, Duration timeout) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<PlaceSummary> searchNearby(NearbySearchQuery query) {
        if (query == null || !validCoordinate(query.latitude(), "latitude") || !validCoordinate(query.longitude(), "longitude")) {
            throw new GooglePlacesException(FailureKind.BAD_RESPONSE, "Invalid coordinates for nearby search.");
        }
        try {
            ObjectNode body = mapper.createObjectNode();
            ObjectNode location = body.putObject("location");
            location.put("latitude", query.latitude());
            location.put("longitude", query.longitude());
            body.put("maxResultCount", Math.min(Math.max(query.maxResultCount(), 1), 20));
            body.put("rankPreference", query.rankPreference() == null ? "DISTANCE" : query.rankPreference().name());
            if (query.includedTypes() != null && !query.includedTypes().isEmpty()) {
                ArrayNode types = body.putArray("includedTypes");
                for (String type : query.includedTypes()) {
                    if (type != null && !type.isBlank()) {
                        types.add(type.trim());
                    }
                }
            }
            if (query.minRating() != null) {
                body.put("minRating", Math.min(Math.max(query.minRating(), 0.0), 5.0));
            }
            if (query.openNow() != null) {
                body.put("openNow", query.openNow());
            }

            JsonNode root = send(HttpRequest.newBuilder(URI.create(baseUrl + "/v1/places:searchNearby"))
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", apiKey)
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build());

            List<PlaceSummary> results = new ArrayList<>();
            JsonNode places = root.get("places");
            if (places != null && places.isArray()) {
                for (JsonNode place : places) {
                    PlaceSummary summary = toSummary(place);
                    if (summary != null) {
                        results.add(summary);
                    }
                }
            }
            return results;
        } catch (GooglePlacesException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GooglePlacesException(FailureKind.TIMEOUT, "Interrupted while contacting Google Places.");
        } catch (Exception e) {
            throw new GooglePlacesException(FailureKind.BAD_RESPONSE, "Unexpected failure while calling Google Places.");
        }
    }

    @Override
    public PlaceDetails fetchDetails(String placeId) {
        if (placeId == null || !PLACE_ID_PATTERN.matcher(placeId).matches()) {
            throw new GooglePlacesException(FailureKind.BAD_RESPONSE, "Invalid place id.");
        }
        String fields = "name,formattedAddress,rating,userRatingCount,businessStatus,location,"
                + "googleMapsUri,nationalPhoneNumber,websiteUri,openingHours";
        try {
            JsonNode root = send(HttpRequest.newBuilder(URI.create(baseUrl + "/v1/places/" + placeId + "?fields=" + fields))
                    .header("X-Goog-Api-Key", apiKey)
                    .timeout(timeout)
                    .GET()
                    .build());

            String name = text(root, "name");
            if (name == null || !validCoordinate(doubleValue(root.get("location"), "latitude"), "latitude")
                    || !validCoordinate(doubleValue(root.get("location"), "longitude"), "longitude")) {
                throw new GooglePlacesException(FailureKind.BAD_RESPONSE, "Provider returned incomplete place details.");
            }
            return new PlaceDetails(
                    text(root, "placeId"),
                    name,
                    text(root, "formattedAddress"),
                    doubleOrNull(root.get("rating")),
                    longOrNull(root.get("userRatingCount")),
                    text(root, "businessStatus"),
                    doubleValue(root.get("location"), "latitude"),
                    doubleValue(root.get("location"), "longitude"),
                    text(root, "googleMapsUri"),
                    text(root, "nationalPhoneNumber"),
                    text(root, "websiteUri"),
                    booleanOrNull(root.path("openingHours").get("openNow")),
                    // Places API (New) does not expose per-review dates; never fabricate one.
                    null);
        } catch (GooglePlacesException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GooglePlacesException(FailureKind.TIMEOUT, "Interrupted while contacting Google Places.");
        } catch (Exception e) {
            throw new GooglePlacesException(FailureKind.BAD_RESPONSE, "Unexpected failure while calling Google Places.");
        }
    }

    private PlaceSummary toSummary(JsonNode place) {
        String placeId = text(place, "placeId");
        if (placeId == null || !PLACE_ID_PATTERN.matcher(placeId).matches()) {
            return null;
        }
        JsonNode location = place.get("location");
        if (!validCoordinate(doubleValue(location, "latitude"), "latitude")
                || !validCoordinate(doubleValue(location, "longitude"), "longitude")) {
            return null;
        }
        String name = text(place.path("displayName"), "text");
        if (name == null) {
            return null;
        }
        return new PlaceSummary(
                placeId,
                name,
                text(place, "formattedAddress"),
                text(place, "primaryType"),
                doubleOrNull(place.get("rating")),
                longOrNull(place.get("userRatingCount")),
                text(place, "businessStatus"),
                doubleValue(location, "latitude"),
                doubleValue(location, "longitude"),
                text(place, "googleMapsUri"),
                text(place, "nationalPhoneNumber"),
                text(place, "websiteUri"),
                booleanOrNull(place.path("openingHours").get("openNow")));
    }

    private JsonNode send(HttpRequest request) throws Exception {
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new GooglePlacesException(FailureKind.TIMEOUT, "Google Places request timed out.");
        }
        int status = response.statusCode();
        if (status == 401 || status == 403 || status == 429) {
            throw new GooglePlacesException(FailureKind.QUOTA_OR_AUTHORIZATION, describeError(status, response.body()));
        }
        if (status < 200 || status >= 300) {
            throw new GooglePlacesException(FailureKind.BAD_RESPONSE, "Google Places returned HTTP " + status + ".");
        }
        try {
            return mapper.readTree(response.body());
        } catch (Exception e) {
            throw new GooglePlacesException(FailureKind.BAD_RESPONSE, "Unparseable response from Google Places.");
        }
    }

    private String describeError(int status, String body) {
        try {
            JsonNode node = mapper.readTree(body);
            String message = node.at("/error/message").asText("");
            if (!message.isBlank()) {
                return "Google Places request failed (HTTP " + status + "): " + truncate(message, 200);
            }
        } catch (Exception ignored) {
            // fall through to the generic message; never echo raw provider payloads
        }
        return "Google Places request failed with HTTP " + status + ".";
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        String value = node.get(field).asText();
        return value.isBlank() ? null : value.trim();
    }

    private static Double doubleOrNull(JsonNode node) {
        if (node == null || !node.isNumber()) {
            return null;
        }
        double value = node.asDouble();
        return Double.isFinite(value) ? value : null;
    }

    private static Long longOrNull(JsonNode node) {
        if (node == null || !node.isIntegralNumber()) {
            return null;
        }
        long value = node.asLong();
        return value >= 0 ? value : null;
    }

    private static Boolean booleanOrNull(JsonNode node) {
        if (node == null || !node.isBoolean()) {
            return null;
        }
        return node.asBoolean();
    }

    private static double doubleValue(JsonNode object, String field) {
        JsonNode value = object == null ? null : object.get(field);
        return value != null && value.isNumber() ? value.asDouble() : Double.NaN;
    }

    private static boolean validCoordinate(double value, String label) {
        if (!Double.isFinite(value)) {
            throw new GooglePlacesException(FailureKind.BAD_RESPONSE, "Invalid " + label + ".");
        }
        return true;
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
