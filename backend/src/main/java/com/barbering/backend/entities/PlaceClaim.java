package com.Barbering.backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A barber's claim on a Google place: the owner attests that the listed
 * business is theirs. Approval copies the live place coordinates and
 * google_place_id onto the member profile (per Places ToS, no other place
 * data is stored long-term).
 */
@Entity
@Table(name = "place_claims")
public class PlaceClaim {

    public enum ClaimStatus {
        PENDING, APPROVED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "barber_profile_id", nullable = false)
    private Long barberProfileId;

    @Column(name = "google_place_id", nullable = false, unique = true)
    private String googlePlaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status = ClaimStatus.PENDING;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected PlaceClaim() {
    }

    public PlaceClaim(Long userId, Long barberProfileId, String googlePlaceId) {
        this.userId = userId;
        this.barberProfileId = barberProfileId;
        this.googlePlaceId = googlePlaceId;
        this.status = ClaimStatus.PENDING;
        this.requestedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getBarberProfileId() {
        return barberProfileId;
    }

    public String getGooglePlaceId() {
        return googlePlaceId;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
