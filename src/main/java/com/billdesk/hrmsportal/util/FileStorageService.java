package com.billdesk.hrmsportal.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.HexFormat;
import java.util.Map;

@Component
public class FileStorageService {

    private final Path rootDir;
    private final long maxSizeBytes;

    /** magic-byte signature -> canonical format */
    private static final Map<String, String> SIGNATURES = Map.of(
            "25504446", "PDF",     // %PDF
            "FFD8FF",   "JPG",     // JPEG SOI
            "89504E47", "PNG"      // \x89PNG
    );

    public FileStorageService(@Value("${app.file.upload-dir}") String uploadDir,
                              @Value("${app.file.max-size-bytes}") long maxSizeBytes) {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxSizeBytes = maxSizeBytes;
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(rootDir);
            System.out.println(">>> File upload directory: " + rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + rootDir, e);
        }
    }

    /** Validates and stores the file. Returns the stored filename (not a full path). */
    public String store(MultipartFile file, Long employeeId, String documentType) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        if (file.getSize() > maxSizeBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File exceeds the maximum size of " + (maxSizeBytes / 1024 / 1024) + "MB");
        }

        String format = detectFormat(file);   // magic bytes, not the extension

        String storedName = employeeId + "_" + documentType + "_"
                + System.currentTimeMillis() + "." + format.toLowerCase();

        Path target = rootDir.resolve(storedName).normalize();

        if (!target.startsWith(rootDir)) {   // paranoia against traversal
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store file");
        }

        return storedName;
    }

    public Resource load(String storedName) {
        Path path = rootDir.resolve(storedName).normalize();

        if (!path.startsWith(rootDir) || !Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found on server");
        }

        try {
            return new InputStreamResource(Files.newInputStream(path));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file");
        }
    }

    /** Best-effort delete — never throws, so it can't roll back a committed transaction. */
    public void deleteQuietly(String storedName) {
        if (storedName == null) return;
        try {
            Path path = rootDir.resolve(storedName).normalize();
            if (path.startsWith(rootDir)) Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("WARN: could not delete file " + storedName + " - " + e.getMessage());
        }
    }

    /** Reads the first bytes and matches a known signature. Extension/content-type are client-controlled and untrusted. */
    private String detectFormat(MultipartFile file) {
        byte[] head = new byte[8];
        try (InputStream in = file.getInputStream()) {
            int read = in.read(head);
            if (read < 4) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is too small or corrupt");
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read file");
        }

        String hex = HexFormat.of().formatHex(head).toUpperCase();

        return SIGNATURES.entrySet().stream()
                .filter(e -> hex.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unsupported file type. Only PDF, JPG and PNG are allowed"));
    }

    public String sanitizeDisplayName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) return "document";
        String name = Paths.get(originalFilename).getFileName().toString();   // strips any path
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}