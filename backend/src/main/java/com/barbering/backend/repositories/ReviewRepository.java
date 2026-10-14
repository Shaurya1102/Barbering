package com.Barbering.backend.repositories;

import com.Barbering.backend.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBarberProfileIdOrderByCreatedAtDesc(Long barberProfileId);
    Optional<Review> findByAppointmentId(Long appointmentId);
    boolean existsByAppointmentId(Long appointmentId);
    @Query("select avg(r.rating) from Review r where r.barberProfileId = :barberId")
    Double averageRating(@Param("barberId") Long barberProfileId);
    long countByBarberProfileId(Long barberProfileId);
}
