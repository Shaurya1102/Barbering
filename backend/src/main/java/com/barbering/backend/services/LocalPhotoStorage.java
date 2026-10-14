package com.Barbering.backend.services;

import com.Barbering.backend.exception.ApiProblem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class LocalPhotoStorage implements PhotoStorage {
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private final Path root;

    public LocalPhotoStorage(@Value("${barbering.upload-dir:./uploads/barber-photos}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public StoredPhoto store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw ApiProblem.badRequest("Select an image to upload.");
        if (file.getSize() > MAX_SIZE) throw ApiProblem.badRequest("Images must be 5 MB or smaller.");
        byte[] bytes = file.getBytes();
        String contentType = detectedType(bytes);
        if (contentType == null) throw ApiProblem.badRequest("Only valid JPEG, PNG, or WebP images are supported.");
        Files.createDirectories(root);
        String key = UUID.randomUUID().toString();
        Path destination = root.resolve(key).normalize();
        if (!destination.startsWith(root)) throw ApiProblem.badRequest("Invalid image key.");
        Files.write(destination, bytes);
        return new StoredPhoto(key, contentType, bytes.length);
    }

    @Override
    public Path resolve(String opaqueKey) throws IOException {
        final UUID parsed;
        try { parsed = UUID.fromString(opaqueKey); }
        catch (RuntimeException ex) { throw ApiProblem.notFound("Image was not found."); }
        Path path = root.resolve(parsed.toString()).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) throw ApiProblem.notFound("Image was not found.");
        return path;
    }

    @Override
    public void delete(String opaqueKey) throws IOException {
        Files.deleteIfExists(resolve(opaqueKey));
    }

    private String detectedType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) return "image/png";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        return null;
    }
}
