package com.documentsearch.indexer_worker.controller;

import com.documentsearch.indexer_worker.elasticsearch.DocumentSearchIndex;
import com.documentsearch.indexer_worker.service.DocumentSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class DocumentSearchController {

    private final DocumentSearchService documentSearchService;

    /**
     * Search documents by query string (searches both metadata and content) - TENANT AWARE
     * GET /api/search?q=query&tenantId=tenant1
     */
    @GetMapping
    public ResponseEntity<List<DocumentSearchIndex>> searchDocuments(
            @RequestParam String q,
            @RequestParam(defaultValue = "default") String tenantId) {
        log.info("Search request received with query: {} for tenant: {}", q, tenantId);
        List<DocumentSearchIndex> results = documentSearchService.searchDocuments(q, tenantId);
        return ResponseEntity.ok(results);
    }

    /**
     * Search documents by file name only - TENANT AWARE
     * GET /api/search/filename?name=example&tenantId=tenant1
     */
    @GetMapping("/filename")
    public ResponseEntity<List<DocumentSearchIndex>> searchByFileName(
            @RequestParam String name,
            @RequestParam(defaultValue = "default") String tenantId) {
        log.info("Search by file name: {} for tenant: {}", name, tenantId);
        List<DocumentSearchIndex> results = documentSearchService.searchByFileName(name, tenantId);
        return ResponseEntity.ok(results);
    }

    /**
     * Search documents by content only - TENANT AWARE
     * GET /api/search/content?text=example&tenantId=tenant1
     */
    @GetMapping("/content")
    public ResponseEntity<List<DocumentSearchIndex>> searchByContent(
            @RequestParam String text,
            @RequestParam(defaultValue = "default") String tenantId) {
        log.info("Search by content: {} for tenant: {}", text, tenantId);
        List<DocumentSearchIndex> results = documentSearchService.searchByContent(text, tenantId);
        return ResponseEntity.ok(results);
    }

    /**
     * Search documents by content type - TENANT AWARE
     * GET /api/search/type?contentType=application/pdf&tenantId=tenant1
     */
    @GetMapping("/type")
    public ResponseEntity<List<DocumentSearchIndex>> searchByContentType(
            @RequestParam String contentType,
            @RequestParam(defaultValue = "default") String tenantId) {
        log.info("Search by content type: {} for tenant: {}", contentType, tenantId);
        List<DocumentSearchIndex> results = documentSearchService.searchByContentType(contentType, tenantId);
        return ResponseEntity.ok(results);
    }

    /**
     * Search documents by file type - TENANT AWARE
     * GET /api/search/filetype?fileType=pdf&tenantId=tenant1
     */
    @GetMapping("/filetype")
    public ResponseEntity<List<DocumentSearchIndex>> searchByFileType(
            @RequestParam String fileType,
            @RequestParam(defaultValue = "default") String tenantId) {
        log.info("Search by file type: {} for tenant: {}", fileType, tenantId);
        List<DocumentSearchIndex> results = documentSearchService.searchByFileType(fileType, tenantId);
        return ResponseEntity.ok(results);
    }

    /**
     * Get all documents for a tenant
     * GET /api/search/tenant?tenantId=tenant1
     */
    @GetMapping("/tenant")
    public ResponseEntity<List<DocumentSearchIndex>> getAllDocumentsByTenant(
            @RequestParam(defaultValue = "default") String tenantId) {
        log.info("Fetching all documents for tenant: {}", tenantId);
        List<DocumentSearchIndex> results = documentSearchService.getAllDocumentsByTenant(tenantId);
        return ResponseEntity.ok(results);
    }

    /**
     * Advanced search with multiple criteria - TENANT AWARE
     * GET /api/search/advanced?fileName=example&content=text&contentType=application/pdf&fileType=pdf&tenantId=tenant1
     */
    @GetMapping("/advanced")
    public ResponseEntity<List<DocumentSearchIndex>> advancedSearch(
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String fileType,
            @RequestParam(defaultValue = "default") String tenantId) {
        log.info("Advanced search - tenant: {}, fileName: {}, content: {}, contentType: {}, fileType: {}",
                 tenantId, fileName, content, contentType, fileType);
        List<DocumentSearchIndex> results = documentSearchService.advancedSearch(fileName, content, contentType, fileType, tenantId);
        return ResponseEntity.ok(results);
    }

    /**
     * Get document by ID - TENANT AWARE
     * GET /api/search/document/{id}?tenantId=tenant1
     */
    @GetMapping("/document/{id}")
    public ResponseEntity<DocumentSearchIndex> getDocumentById(
            @PathVariable String id,
            @RequestParam(defaultValue = "default") String tenantId) {
        log.info("Fetching document by ID: {} for tenant: {}", id, tenantId);
        DocumentSearchIndex document = documentSearchService.getDocumentById(id, tenantId);
        if (document != null) {
            return ResponseEntity.ok(document);
        }
        return ResponseEntity.notFound().build();
    }
}
