package com.Barbering.backend.services.google;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Wires the Google Places boundary. With no API key configured the honest
 * unavailable provider is used and nearby discovery reports that state instead
 * of failing or fabricating results.
 */
@Configuration
public class GooglePlacesConfig {

    // Inferred destroy method: Spring calls close()/shutdown() only when the
    // concrete instance declares one (HttpGooglePlacesProvider does, the
    // Unavailable provider does not). An explicit name would be validated
    // against the GooglePlacesProvider interface type and fail context load.
    @Bean(destroyMethod = "")
    public GooglePlacesProvider googlePlacesProvider(
            @Value("${barbering.google.places.api-key:}") String apiKey,
            @Value("${barbering.google.places.base-url:https://places.googleapis.com}") String baseUrl,
            @Value("${barbering.google.places.timeout-seconds:10}") long timeoutSeconds) {
        if (apiKey == null || apiKey.isBlank()) {
            return new UnavailableGooglePlacesProvider();
        }
        Duration timeout = Duration.ofSeconds(Math.max(2, Math.min(timeoutSeconds, 30)));
        return new HttpGooglePlacesProvider(apiKey.trim(), baseUrl, timeout);
    }
}
