package com.Barbering.backend.controllers;

import com.Barbering.backend.dto.MarketplaceDtos.FavouriteDto;
import com.Barbering.backend.security.UserPrincipal;
import com.Barbering.backend.services.FavouriteService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/favourites")
@PreAuthorize("hasRole('CUSTOMER')")
public class FavouriteController {
    private final FavouriteService favourites;
    public FavouriteController(FavouriteService favourites) { this.favourites = favourites; }

    @GetMapping
    public List<FavouriteDto> list(@AuthenticationPrincipal UserPrincipal user) { return favourites.list(user.id()); }

    @PutMapping("/{barberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long barberId) {
        favourites.add(user.id(), barberId);
    }

    @DeleteMapping("/{barberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long barberId) {
        favourites.remove(user.id(), barberId);
    }
}
