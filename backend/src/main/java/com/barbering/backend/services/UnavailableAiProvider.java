package com.Barbering.backend.services;

import com.Barbering.backend.dto.MarketplaceDtos.RecommendationRequest;
import com.Barbering.backend.dto.MarketplaceDtos.RecommendationResponse;
import com.Barbering.backend.dto.MarketplaceDtos.TryOnRequest;
import com.Barbering.backend.dto.MarketplaceDtos.TryOnResponse;
import com.Barbering.backend.exception.ApiProblem;
import org.springframework.stereotype.Component;

@Component
public class UnavailableAiProvider implements AiProvider {
    @Override
    public RecommendationResponse recommend(RecommendationRequest request) {
        throw ApiProblem.unavailable("AI recommendations are not configured yet. Your details were not sent to an external service.");
    }

    @Override
    public TryOnResponse tryOn(TryOnRequest request) {
        throw ApiProblem.unavailable("Hairstyle image generation is not configured yet. No image was processed.");
    }
}
