package com.Barbering.backend.controllers;

import com.Barbering.backend.dto.DiscoveryDtos.PlaceClaimDto;
import com.Barbering.backend.security.UserPrincipal;
import com.Barbering.backend.services.discovery.PlaceClaimService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Owner-attested Google place claims for the authenticated barber. */
@RestController
public class PlaceClaimController {

    public record ClaimRequest(@NotBlank String googlePlaceId) {
    }

    private final PlaceClaimService claimService;

    public PlaceClaimController(PlaceClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping("/barber/me/google-claim")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BARBER')")
    public PlaceClaimDto request(@AuthenticationPrincipal UserPrincipal user, @RequestBody ClaimRequest body) {
        return claimService.request(user.id(), body.googlePlaceId());
    }

    @GetMapping("/barber/me/google-claim")
    @PreAuthorize("hasRole('BARBER')")
    public PlaceClaimDto current(@AuthenticationPrincipal UserPrincipal user) {
        return claimService.current(user.id());
    }

    @PostMapping("/barber/me/google-claim/{claimId}/approve")
    @PreAuthorize("hasRole('BARBER')")
    public PlaceClaimDto approve(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long claimId) {
        return claimService.approve(user.id(), claimId);
    }

    @PostMapping("/barber/me/google-claim/{claimId}/reject")
    @PreAuthorize("hasRole('BARBER')")
    public PlaceClaimDto reject(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long claimId) {
        return claimService.reject(user.id(), claimId);
    }
}
