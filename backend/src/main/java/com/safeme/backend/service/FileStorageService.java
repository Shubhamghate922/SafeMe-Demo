package com.safeme.backend.service;

import com.safeme.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "m4a", "mp3", "wav", "aac");
    private final Path uploadRoot;

    public FileStorageService(@Value("${safeme.uploads.dir:./uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not initialize uploads directory", ex);
        }
    }

    public String storeEvidenceFile(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String original = file.getOriginalFilename();
        String ext = extractSafeExtension(original);
        String filename = prefix + "_" + UUID.randomUUID() + "." + ext;
        Path target = uploadRoot.resolve(filename).normalize();
        if (!target.startsWith(uploadRoot + java.io.File.separator)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file path.");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + filename;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store uploaded evidence file.");
        }
    }

    private String extractSafeExtension(String originalFilename) {
        if (originalFilename == null) return "bin";
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) return "bin";
        String candidate = originalFilename.substring(dot + 1).toLowerCase().replaceAll("[^a-z0-9]", "");
        return ALLOWED_EXTENSIONS.contains(candidate) ? candidate : "bin";
    }
}
