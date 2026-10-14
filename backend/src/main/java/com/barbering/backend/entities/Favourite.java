package com.Barbering.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "favourites", uniqueConstraints = @UniqueConstraint(name = "uq_favourites_customer_barber", columnNames = {"customer_id", "barber_profile_id"}))
@Getter @NoArgsConstructor
public class Favourite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    @Column(name = "barber_profile_id", nullable = false)
    private Long barberProfileId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Favourite(Long customerId, Long barberProfileId) {
        this.customerId = customerId;
        this.barberProfileId = barberProfileId;
    }
}
