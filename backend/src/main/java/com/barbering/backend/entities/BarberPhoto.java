package com.Barbering.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "barber_photos")
@Getter @NoArgsConstructor
public class BarberPhoto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "barber_profile_id", nullable = false)
    private Long barberProfileId;
    @Column(name = "storage_key", nullable = false, unique = true, length = 36)
    private String storageKey;
    @Column(name = "content_type", nullable = false, length = 40)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    @Column(name = "alt_text", length = 160)
    private String altText;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public BarberPhoto(Long barberProfileId, String storageKey, String contentType, long sizeBytes, String altText) {
        this.barberProfileId = barberProfileId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.altText = altText;
    }
}
