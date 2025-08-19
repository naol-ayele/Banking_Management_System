package com.BMS.Bank_Management_System.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final Path rootDir = Paths.get("uploads", "national-ids");
    private final Path legalDocsDir = Paths.get("uploads", "legal-documents");

    public FileStorageService() throws IOException {
        Files.createDirectories(rootDir);
        Files.createDirectories(legalDocsDir);
    }

    public String storeNationalId(Long userId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type");
        }

        String original = file.getOriginalFilename();
        String ext = ".bin";
        if (original != null) {
            int dot = original.lastIndexOf('.');
            if (dot >= 0 && dot < original.length() - 1) {
                ext = original.substring(dot).toLowerCase();
            }
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String filename = userId + "_" + timestamp + ext;
        Path target = rootDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

    public String storeLegalDocument(Long userId, MultipartFile file, String documentType) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Allow more document types for legal documents
        Set<String> legalDocTypes = Set.of(
                "image/jpeg", "image/png", "image/gif", "image/webp",
                "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );

        String contentType = file.getContentType();
        if (contentType == null || !legalDocTypes.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type for legal document");
        }

        String original = file.getOriginalFilename();
        String ext = ".bin";
        if (original != null) {
            int dot = original.lastIndexOf('.');
            if (dot >= 0 && dot < original.length() - 1) {
                ext = original.substring(dot).toLowerCase();
            }
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String filename = userId + "_" + documentType + "_" + timestamp + ext;
        Path target = legalDocsDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

    public byte[] readFile(String pathString) throws IOException {
        Path p = Paths.get(pathString);
        return Files.readAllBytes(p);
    }
}



