package com.Barbering.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "barber_services")
@Getter @Setter @NoArgsConstructor
public class BarberServiceOffering {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "barber_profile_id", nullable = false)
    private Long barberProfileId;
    @Column(name = "service_id", nullable = false)
    private Long serviceId;
    @Column(name = "name_override", length = 100)
    private String nameOverride;
    @Column(name = "description_override", length = 1000)
    private String descriptionOverride;
    @Column(name = "price_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAmount;
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public BarberServiceOffering(Long barberProfileId, Long serviceId, BigDecimal priceAmount, int durationMinutes) {
        this.barberProfileId = barberProfileId;
        this.serviceId = serviceId;
        this.priceAmount = priceAmount;
        this.durationMinutes = durationMinutes;
    }
    public void touch() { updatedAt = Instant.now(); }
}
