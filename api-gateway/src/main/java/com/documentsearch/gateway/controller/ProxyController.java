package com.documentsearch.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProxyController {

    @Value("${backend.document-management-service.url}")
    private String documentManagementServiceUrl;

    @Value("${backend.document-search-service.url:http://document-search-service:8082}")
    private String documentSearchServiceUrl;

    private final WebClient.Builder webClientBuilder;

    /**
     * Upload document - extracts tenantId from JWT token
     * POST /api/documents
     */
    @PostMapping("/api/documents")
    public ResponseEntity<?> uploadDocument(
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {

        String tenantId = (String) request.getAttribute("tenantId");
        String username = (String) request.getAttribute("username");

        log.info("[API Gateway] User '{}' from tenant '{}' uploading document", username, tenantId);

        try {
            // Validate file on gateway level
            if (file == null || file.isEmpty()) {
                log.warn("[API Gateway] Empty file upload attempt by user: {}", username);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse(400, "Bad Request",
                              "File is required and cannot be empty", "/api/documents"));
            }

            log.info("[API Gateway] Forwarding document '{}' ({} bytes) to document-management-service",
                    file.getOriginalFilename(), file.getSize());

            // Build multipart request
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource());
            builder.part("tenantId", tenantId);

            // Forward to document-management-service
            WebClient webClient = webClientBuilder.baseUrl(documentManagementServiceUrl).build();

            String response = webClient.post()
                    .uri("/documents")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("[API Gateway] Document upload successful for user: {}", username);
            return ResponseEntity.accepted().body(response);

        } catch (WebClientResponseException e) {
            log.error("[API Gateway] Backend service returned error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[API Gateway] Error uploading document for user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(500, "Internal Server Error",
                          "Failed to upload document: " + e.getMessage(), "/api/documents"));
        }
    }

    /**
     * Search documents - extracts tenantId from JWT token
     * GET /api/search
     */
    @GetMapping("/api/search")
    public ResponseEntity<?> searchDocuments(
            @RequestParam(required = false) String q,
            HttpServletRequest request) {

        String tenantId = (String) request.getAttribute("tenantId");
        String username = (String) request.getAttribute("username");

        log.info("[API Gateway] User '{}' from tenant '{}' searching for: '{}'", username, tenantId, q);

        try {
            // Validate query on gateway level
            if (q == null || q.trim().isEmpty()) {
                log.warn("[API Gateway] Empty search query from user: {}", username);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse(400, "Bad Request",
                              "Search query 'q' is required and cannot be empty", "/api/search"));
            }

            log.info("[API Gateway] Forwarding search request to document-search-service");

            WebClient webClient = webClientBuilder.baseUrl(documentSearchServiceUrl).build();

            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/search")
                            .queryParam("q", q)
                            .queryParam("tenant", tenantId)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("[API Gateway] Search completed successfully for user: {}", username);
            return ResponseEntity.ok(response);

        } catch (WebClientResponseException e) {
            log.error("[API Gateway] Backend service returned error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[API Gateway] Error searching documents for user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(500, "Internal Server Error",
                          "Failed to perform search: " + e.getMessage(), "/api/search"));
        }
    }

    /**
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(int status, String error, String message, String path) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", status);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("path", path);
        return errorResponse;
    }
}
