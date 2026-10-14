package com.Barbering.backend.repositories;

import com.Barbering.backend.entities.Appointment;
import com.Barbering.backend.entities.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @Query("""
            select a from Appointment a where a.barberProfileId = :barberId
              and a.status in :statuses and a.startAt < :end and a.endAt > :start
              and (:excludeId is null or a.id <> :excludeId)
            """)
    List<Appointment> findOverlapping(@Param("barberId") Long barberId,
                                      @Param("start") OffsetDateTime start,
                                      @Param("end") OffsetDateTime end,
                                      @Param("statuses") Collection<AppointmentStatus> statuses,
                                      @Param("excludeId") Long excludeId);

    List<Appointment> findByCustomerIdOrderByStartAtDesc(Long customerId);
    List<Appointment> findByBarberProfileIdOrderByStartAtAsc(Long barberProfileId);
    Optional<Appointment> findByIdAndCustomerId(Long id, Long customerId);
    Optional<Appointment> findByIdAndBarberProfileId(Long id, Long barberProfileId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id and a.customerId = :customerId")
    Optional<Appointment> lockByIdAndCustomerId(@Param("id") Long id, @Param("customerId") Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id and a.barberProfileId = :barberProfileId")
    Optional<Appointment> lockByIdAndBarberProfileId(@Param("id") Long id, @Param("barberProfileId") Long barberProfileId);

    @Query("select coalesce(sum(a.priceAmount), 0) from Appointment a where a.barberProfileId = :barberId and a.status = :status and a.startAt >= :from and a.startAt < :to")
    BigDecimal sumEarnings(@Param("barberId") Long barberId, @Param("status") AppointmentStatus status,
                           @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
