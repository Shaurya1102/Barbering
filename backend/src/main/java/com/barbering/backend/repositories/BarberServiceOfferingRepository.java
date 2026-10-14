package com.Barbering.backend.repositories;

import com.Barbering.backend.entities.BarberServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BarberServiceOfferingRepository extends JpaRepository<BarberServiceOffering, Long> {
    List<BarberServiceOffering> findByBarberProfileIdAndEnabledTrueOrderById(Long barberProfileId);
    List<BarberServiceOffering> findByBarberProfileIdOrderById(Long barberProfileId);
    Optional<BarberServiceOffering> findByIdAndBarberProfileId(Long id, Long barberProfileId);
    boolean existsByBarberProfileIdAndServiceId(Long barberProfileId, Long serviceId);
}
