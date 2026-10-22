package com.Barbering.backend.controllers;

import com.Barbering.backend.dto.DiscoveryDtos.DiscoveryResult;
import com.Barbering.backend.services.discovery.DiscoveryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public read-only discovery: nearby barbers from member profiles + Google Places. */
@RestController
public class DiscoveryController {

    private final DiscoveryService discovery;

    public DiscoveryController(DiscoveryService discovery) {
        this.discovery = discovery;
    }

    @GetMapping("/discovery/nearby")
    public DiscoveryResult nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "25") double radiusKm,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false, defaultValue = "false") boolean openNow,
            @RequestParam(required = false) List<String> services,
            @RequestParam(required = false, defaultValue = "false") boolean bookable) {
        return discovery.nearby(lat, lng, radiusKm, minRating, openNow, services != null ? services : List.of(), bookable);
    }
}
