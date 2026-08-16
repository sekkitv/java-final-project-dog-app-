package com.zuzdog.service;

import com.zuzdog.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

// saves uploaded images to disk under app.upload.dir, returns the /uploads/... URL
@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    // validates, saves the file under uploadRoot/subdirectory/, returns its public URL
    public String store(MultipartFile file, String subdirectory) {
        validate(file);

        String extension = extractExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;
        Path targetDir = uploadRoot.resolve(subdirectory);
        Path targetFile = targetDir.resolve(filename);

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }

        return "/uploads/" + subdirectory + "/" + filename;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File exceeds max size of 5MB");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported file type: " + file.getContentType());
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        return dotIndex >= 0 ? originalFilename.substring(dotIndex) : "";
    }
}
