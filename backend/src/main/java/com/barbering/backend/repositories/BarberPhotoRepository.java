package com.Barbering.backend.repositories;

import com.Barbering.backend.entities.BarberPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BarberPhotoRepository extends JpaRepository<BarberPhoto, Long> {
    List<BarberPhoto> findByBarberProfileIdOrderByCreatedAtDesc(Long barberProfileId);
    Optional<BarberPhoto> findByIdAndBarberProfileId(Long id, Long barberProfileId);
}
