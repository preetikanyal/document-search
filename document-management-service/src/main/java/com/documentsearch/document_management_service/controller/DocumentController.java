package com.documentsearch.document_management_service.controller;

import com.documentsearch.document_management_service.dto.DocumentUploadResponse;
import com.documentsearch.document_management_service.dto.ErrorResponse;
import com.documentsearch.document_management_service.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/documents")
@Slf4j
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tenantId", required = false, defaultValue = "default") String tenantId) {
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                log.warn("Empty file upload attempt for tenant: {}", tenantId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(
                                400,
                                "Bad Request",
                                "File is required and cannot be empty",
                                "/documents"
                        ));
            }

            // Validate file size (e.g., max 50MB)
            long maxSize = 50 * 1024 * 1024; // 50MB
            if (file.getSize() > maxSize) {
                log.warn("File too large: {} bytes for tenant: {}", file.getSize(), tenantId);
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body(new ErrorResponse(
                                413,
                                "Payload Too Large",
                                "File size exceeds maximum allowed size of 50MB",
                                "/documents"
                        ));
            }

            // Validate file name
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                log.warn("Invalid filename for tenant: {}", tenantId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(
                                400,
                                "Bad Request",
                                "File must have a valid filename",
                                "/documents"
                        ));
            }

            log.info("Processing document upload: {} ({} bytes) for tenant: {}",
                    originalFilename, file.getSize(), tenantId);

            DocumentUploadResponse response = documentService.uploadDocument(file, tenantId);

            log.info("Document uploaded successfully: ID={}, fileName={}, tenant={}",
                    response.getDocumentId(), response.getFileName(), tenantId);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(
                            400,
                            "Bad Request",
                            e.getMessage(),
                            "/documents"
                    ));
        } catch (Exception e) {
            log.error("Error processing document upload for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(
                            500,
                            "Internal Server Error",
                            "Failed to upload document: " + e.getMessage(),
                            "/documents"
                    ));
        }
    }
}
