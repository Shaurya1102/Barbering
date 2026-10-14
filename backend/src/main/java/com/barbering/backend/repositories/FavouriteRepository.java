package com.Barbering.backend.repositories;

import com.Barbering.backend.entities.Favourite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavouriteRepository extends JpaRepository<Favourite, Long> {
    List<Favourite> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    Optional<Favourite> findByCustomerIdAndBarberProfileId(Long customerId, Long barberProfileId);
}
