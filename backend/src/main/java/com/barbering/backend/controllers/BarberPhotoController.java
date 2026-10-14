package com.Barbering.backend.controllers;

import com.Barbering.backend.dto.MarketplaceDtos.BarberPhotoDto;
import com.Barbering.backend.security.UserPrincipal;
import com.Barbering.backend.services.BarberPhotoService;
import com.Barbering.backend.services.BarberPhotoService.PhotoResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
public class BarberPhotoController {
    private final BarberPhotoService photos;
    public BarberPhotoController(BarberPhotoService photos) { this.photos = photos; }

    @PostMapping(value = "/barber/me/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('BARBER')")
    public BarberPhotoDto upload(@AuthenticationPrincipal UserPrincipal user,
                                 @RequestPart("file") MultipartFile file,
                                 @RequestPart(value = "altText", required = false) String altText) {
        return photos.upload(user.id(), file, altText);
    }

    @DeleteMapping("/barber/me/photos/{photoId}")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long photoId) {
        photos.delete(user.id(), photoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/barber-photos/{photoId}/content")
    public ResponseEntity<FileSystemResource> content(@PathVariable Long photoId) {
        PhotoResource photo = photos.content(photoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(photo.path()));
    }
}
