package com.documentsearch.document_management_service.service;

import com.documentsearch.document_management_service.dto.DocumentIndexMessage;
import com.documentsearch.document_management_service.dto.DocumentUploadResponse;
import com.documentsearch.document_management_service.entity.Document;
import com.documentsearch.document_management_service.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private RabbitMQPublisher rabbitMQPublisher;

    @Value("${document.storage.path:./document-storage}")
    private String storagePath;

    public DocumentUploadResponse uploadDocument(MultipartFile file, String tenantId) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            // Validate tenantId
            if (tenantId == null || tenantId.trim().isEmpty()) {
                throw new IllegalArgumentException("TenantId is required");
            }

            // Create storage directory if not exists
            Path storageDirectory = Paths.get(storagePath);
            if (!Files.exists(storageDirectory)) {
                Files.createDirectories(storageDirectory);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
            Path filePath = storageDirectory.resolve(uniqueFilename);

            // Save file to local filesystem
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved to: {}", filePath.toAbsolutePath());

            // Extract file type from filename
            String fileType = extractFileType(originalFilename);

            // Save metadata to database
            Document document = new Document();
            document.setFileName(originalFilename);
            document.setFilePath(filePath.toAbsolutePath().toString());
            document.setContentType(file.getContentType());
            document.setFileType(fileType);
            document.setFileSize(file.getSize());
            document.setTenantId(tenantId);
            document.setUploadedAt(LocalDateTime.now());
            document.setStatus("UPLOADED");

            document = documentRepository.save(document);
            log.info("Document metadata saved to database with ID: {} for tenant: {}", document.getId(), tenantId);

            // Publish message to RabbitMQ for async processing
            DocumentIndexMessage message = new DocumentIndexMessage(
                    document.getId(),
                    document.getFileName(),
                    document.getFilePath(),
                    document.getContentType(),
                    document.getFileSize(),
                    document.getUploadedAt()
            );

            rabbitMQPublisher.publishDocumentIndexMessage(message);
            log.info("Document index message published to RabbitMQ for document ID: {}", document.getId());

            // Return response
            return new DocumentUploadResponse(
                    document.getId(),
                    document.getFileName(),
                    document.getStatus(),
                    document.getUploadedAt(),
                    "Document uploaded successfully and queued for indexing"
            );

        } catch (IOException e) {
            log.error("Error saving file", e);
            throw new RuntimeException("Failed to store file", e);
        }
    }

    /**
     * Extract file type/extension from filename
     * Supports: pdf, doc, docx, xls, xlsx, ppt, pptx, txt, csv, html, xml, json, etc.
     */
    private String extractFileType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unknown";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }

        return "unknown";
    }
}
