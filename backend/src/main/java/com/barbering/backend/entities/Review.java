package com.Barbering.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "appointment_id", nullable = false, unique = true)
    private Long appointmentId;
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    @Column(name = "barber_profile_id", nullable = false)
    private Long barberProfileId;
    @Column(nullable = false)
    private int rating;
    @Column(name = "review_text", length = 2000)
    private String reviewText;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Review(Long appointmentId, Long customerId, Long barberProfileId, int rating, String reviewText) {
        this.appointmentId = appointmentId;
        this.customerId = customerId;
        this.barberProfileId = barberProfileId;
        this.rating = rating;
        this.reviewText = reviewText;
    }
}
