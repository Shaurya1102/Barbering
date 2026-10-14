package com.Barbering.backend.controllers;

import com.Barbering.backend.dto.MarketplaceDtos.RecommendationRequest;
import com.Barbering.backend.dto.MarketplaceDtos.RecommendationResponse;
import com.Barbering.backend.dto.MarketplaceDtos.TryOnRequest;
import com.Barbering.backend.dto.MarketplaceDtos.TryOnResponse;
import com.Barbering.backend.services.AiProvider;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@PreAuthorize("isAuthenticated()")
public class AiController {
    private final AiProvider provider;
    public AiController(AiProvider provider) { this.provider = provider; }

    @PostMapping("/recommendations")
    public RecommendationResponse recommend(@Valid @RequestBody RecommendationRequest request) {
        return provider.recommend(request);
    }

    @PostMapping("/try-on")
    public TryOnResponse tryOn(@Valid @RequestBody TryOnRequest request) {
        return provider.tryOn(request);
    }
}
