package com.Barbering.backend.repositories;

import com.Barbering.backend.entities.WorkingHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkingHourRepository extends JpaRepository<WorkingHour, Long> {
    List<WorkingHour> findByBarberProfileIdOrderByDayOfWeek(Long barberProfileId);
    java.util.Optional<WorkingHour> findByBarberProfileIdAndDayOfWeek(Long barberProfileId, int dayOfWeek);
    void deleteByBarberProfileId(Long barberProfileId);
}
