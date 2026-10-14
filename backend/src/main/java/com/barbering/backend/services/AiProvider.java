package com.Barbering.backend.services;

import com.Barbering.backend.dto.MarketplaceDtos.RecommendationRequest;
import com.Barbering.backend.dto.MarketplaceDtos.RecommendationResponse;
import com.Barbering.backend.dto.MarketplaceDtos.TryOnRequest;
import com.Barbering.backend.dto.MarketplaceDtos.TryOnResponse;

/** Replaceable boundary for a configured, privacy-reviewed AI provider. */
public interface AiProvider {
    RecommendationResponse recommend(RecommendationRequest request);
    TryOnResponse tryOn(TryOnRequest request);
}
