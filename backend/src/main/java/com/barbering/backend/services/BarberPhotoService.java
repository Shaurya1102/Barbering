package com.Barbering.backend.services;

import com.Barbering.backend.dto.MarketplaceDtos.BarberPhotoDto;
import com.Barbering.backend.entities.BarberPhoto;
import com.Barbering.backend.entities.BarberProfile;
import com.Barbering.backend.exception.ApiProblem;
import com.Barbering.backend.repositories.BarberPhotoRepository;
import com.Barbering.backend.repositories.BarberProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@Service
@Transactional
public class BarberPhotoService {
    private final BarberProfileRepository profiles;
    private final BarberPhotoRepository photos;
    private final PhotoStorage storage;

    public BarberPhotoService(BarberProfileRepository profiles, BarberPhotoRepository photos, PhotoStorage storage) {
        this.profiles = profiles;
        this.photos = photos;
        this.storage = storage;
    }

    public BarberPhotoDto upload(Long userId, MultipartFile file, String altText) {
        String normalizedAltText = clean(altText);
        if (normalizedAltText != null && normalizedAltText.length() > 160) {
            throw ApiProblem.badRequest("Image descriptions must be 160 characters or fewer.");
        }
        BarberProfile profile = profiles.findByUserId(userId)
                .orElseThrow(() -> ApiProblem.notFound("Barber profile was not found."));
        try {
            PhotoStorage.StoredPhoto stored = storage.store(file);
            try {
                BarberPhoto photo = photos.saveAndFlush(new BarberPhoto(profile.getId(), stored.key(), stored.contentType(), stored.sizeBytes(), normalizedAltText));
                return toDto(photo);
            } catch (RuntimeException ex) {
                try { storage.delete(stored.key()); }
                catch (IOException cleanupFailure) { ex.addSuppressed(cleanupFailure); }
                throw ex;
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Image storage is unavailable.", ex);
        }
    }

    public void delete(Long userId, Long photoId) {
        BarberProfile profile = profiles.findByUserId(userId)
                .orElseThrow(() -> ApiProblem.notFound("Barber profile was not found."));
        BarberPhoto photo = photos.findByIdAndBarberProfileId(photoId, profile.getId())
                .orElseThrow(() -> ApiProblem.notFound("Image was not found."));
        try {
            storage.delete(photo.getStorageKey());
            photos.delete(photo);
        } catch (IOException ex) {
            throw new IllegalStateException("Image storage is unavailable.", ex);
        }
    }

    @Transactional(readOnly = true)
    public PhotoResource content(Long photoId) {
        BarberPhoto photo = photos.findById(photoId).orElseThrow(() -> ApiProblem.notFound("Image was not found."));
        try { return new PhotoResource(storage.resolve(photo.getStorageKey()), photo.getContentType()); }
        catch (IOException ex) { throw new IllegalStateException("Image storage is unavailable.", ex); }
    }

    private BarberPhotoDto toDto(BarberPhoto photo) {
        return new BarberPhotoDto(photo.getId(), "/barber-photos/" + photo.getId() + "/content",
                photo.getContentType(), photo.getSizeBytes(), photo.getAltText());
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record PhotoResource(Path path, String contentType) {}
}
