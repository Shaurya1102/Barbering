package com.Barbering.backend.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface PhotoStorage {
    StoredPhoto store(MultipartFile file) throws IOException;
    Path resolve(String opaqueKey) throws IOException;
    void delete(String opaqueKey) throws IOException;

    record StoredPhoto(String key, String contentType, long sizeBytes) {}
}
