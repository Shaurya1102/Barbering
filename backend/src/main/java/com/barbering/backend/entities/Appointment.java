package com.Barbering.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

@Entity
@Table(name = "appointments")
@Getter @Setter @NoArgsConstructor
public class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    @Column(name = "barber_profile_id", nullable = false)
    private Long barberProfileId;
    @Column(name = "barber_service_id", nullable = false)
    private Long barberServiceId;
    @Column(name = "service_name_snapshot", nullable = false, length = 100)
    private String serviceNameSnapshot;
    @Column(name = "price_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAmount;
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;
    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;
    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.PENDING;
    @Column(name = "customer_note", length = 1000)
    private String customerNote;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public void touch() { updatedAt = Instant.now(); }
}
