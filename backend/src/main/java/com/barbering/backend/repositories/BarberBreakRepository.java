package com.Barbering.backend.repositories;

import com.Barbering.backend.entities.BarberBreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BarberBreakRepository extends JpaRepository<BarberBreak, Long> {
    List<BarberBreak> findByBarberProfileIdAndDayOfWeekOrderByStartTime(Long barberProfileId, int dayOfWeek);
    List<BarberBreak> findByBarberProfileIdOrderByDayOfWeekAscStartTimeAsc(Long barberProfileId);
    Optional<BarberBreak> findByIdAndBarberProfileId(Long id, Long barberProfileId);
}
