package com.Barbering.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "barber_profiles")
@Getter
@Setter
@NoArgsConstructor
public class BarberProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    @Column(length = 2000)
    private String bio;
    @Column(length = 120)
    private String city;
    @Column(length = 120)
    private String region;
    @Column(name = "street_address", length = 255)
    private String streetAddress;
    @Column(name = "postal_code", length = 32)
    private String postalCode;
    @Column(length = 120)
    private String country;
    @Column(name = "timezone_id", nullable = false, length = 80)
    private String timezoneId = "UTC";
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public BarberProfile(Long userId) { this.userId = userId; }
    public void touch() { updatedAt = Instant.now(); }
}
