package com.documentsearch.document_search_service.controller;

import com.documentsearch.document_search_service.dto.ErrorResponse;
import com.documentsearch.document_search_service.dto.SearchResponse;
import com.documentsearch.document_search_service.dto.SearchResultDTO;
import com.documentsearch.document_search_service.service.DocumentSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final DocumentSearchService documentSearchService;

    /**
     * Hybrid search endpoint
     * GET /api/search?q={query}&tenant={tenantId}
     *
     * Performs hybrid search across document metadata (filename) and content,
     * filtered by tenant ID.
     *
     * @param query Search query string
     * @param tenantId Tenant ID for multi-tenancy support
     * @return List of matching documents with relevance scores
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(required = false) String q,
            @RequestParam String tenant) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Search request - query: '{}', tenant: {}", q, tenant);

            // Validate tenant ID
            if (tenant == null || tenant.trim().isEmpty()) {
                log.warn("Search request rejected: Tenant ID is required but not provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(
                                400,
                                "Bad Request",
                                "Tenant ID is required. Please provide 'tenant' parameter.",
                                "/api/search"
                        ));
            }

            // Validate query
            if (q == null || q.trim().isEmpty()) {
                log.warn("Search request rejected: Query string is empty for tenant: {}", tenant);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(
                                400,
                                "Bad Request",
                                "Search query 'q' is required and cannot be empty.",
                                "/api/search"
                        ));
            }

            // Validate query length (e.g., max 500 characters)
            if (q.length() > 500) {
                log.warn("Search request rejected: Query too long ({} chars) for tenant: {}", q.length(), tenant);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(
                                400,
                                "Bad Request",
                                "Search query is too long. Maximum allowed length is 500 characters.",
                                "/api/search"
                        ));
            }

            // Perform search
            List<SearchResultDTO> results = documentSearchService.hybridSearch(q, tenant);
            long searchTimeMs = System.currentTimeMillis() - startTime;

            log.info("Search completed successfully: {} result(s) found in {}ms for tenant: {}",
                    results.size(), searchTimeMs, tenant);

            SearchResponse response = new SearchResponse(results, q, tenant, searchTimeMs);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid search request for tenant {}: {}", tenant, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(
                            400,
                            "Bad Request",
                            e.getMessage(),
                            "/api/search"
                    ));
        } catch (Exception e) {
            log.error("Error performing search for tenant {}, query '{}': {}", tenant, q, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(
                            500,
                            "Internal Server Error",
                            "Failed to perform search: " + e.getMessage(),
                            "/api/search"
                    ));
        }
    }
}
