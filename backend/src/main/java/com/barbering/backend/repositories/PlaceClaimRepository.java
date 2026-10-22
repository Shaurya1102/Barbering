package com.Barbering.backend.repositories;

import com.Barbering.backend.entities.PlaceClaim;
import com.Barbering.backend.entities.PlaceClaim.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlaceClaimRepository extends JpaRepository<PlaceClaim, Long> {

    Optional<PlaceClaim> findByGooglePlaceId(String googlePlaceId);

    List<PlaceClaim> findByBarberProfileId(Long barberProfileId);

    List<PlaceClaim> findByBarberProfileIdAndStatusIn(Long barberProfileId, Collection<ClaimStatus> statuses);
}
