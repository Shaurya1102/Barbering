package com.Barbering.backend.services;

import com.Barbering.backend.dto.MarketplaceDtos.FavouriteDto;
import com.Barbering.backend.entities.BarberProfile;
import com.Barbering.backend.entities.Favourite;
import com.Barbering.backend.entities.User;
import com.Barbering.backend.exception.ApiProblem;
import com.Barbering.backend.repositories.BarberProfileRepository;
import com.Barbering.backend.repositories.FavouriteRepository;
import com.Barbering.backend.repositories.ReviewRepository;
import com.Barbering.backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FavouriteService {
    private final FavouriteRepository favourites;
    private final BarberProfileRepository profiles;
    private final UserRepository users;
    private final ReviewRepository reviews;

    public FavouriteService(FavouriteRepository favourites, BarberProfileRepository profiles,
                            UserRepository users, ReviewRepository reviews) {
        this.favourites = favourites;
        this.profiles = profiles;
        this.users = users;
        this.reviews = reviews;
    }

    public void add(Long customerId, Long barberId) {
        profiles.lockById(barberId).orElseThrow(() -> ApiProblem.notFound("Barber was not found."));
        if (favourites.findByCustomerIdAndBarberProfileId(customerId, barberId).isEmpty()) {
            favourites.save(new Favourite(customerId, barberId));
        }
    }

    public void remove(Long customerId, Long barberId) {
        favourites.findByCustomerIdAndBarberProfileId(customerId, barberId).ifPresent(favourites::delete);
    }

    @Transactional(readOnly = true)
    public List<FavouriteDto> list(Long customerId) {
        return favourites.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(favourite -> {
            BarberProfile profile = profiles.findById(favourite.getBarberProfileId())
                    .orElseThrow(() -> ApiProblem.notFound("Barber was not found."));
            User barber = users.findById(profile.getUserId()).orElseThrow(() -> ApiProblem.notFound("Barber was not found."));
            Double avg = reviews.averageRating(profile.getId());
            return new FavouriteDto(profile.getId(), barber.getDisplayName(), profile.getCity(),
                    avg == null ? 0.0 : avg, reviews.countByBarberProfileId(profile.getId()));
        }).toList();
    }
}
