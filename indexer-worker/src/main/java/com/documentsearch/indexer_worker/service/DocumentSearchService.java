package com.documentsearch.indexer_worker.service;

import com.documentsearch.indexer_worker.elasticsearch.DocumentSearchIndex;
import com.documentsearch.indexer_worker.elasticsearch.DocumentSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentSearchService {

    private final DocumentSearchRepository documentSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * Search documents by query string in both metadata and content (tenant-aware)
     */
    public List<DocumentSearchIndex> searchDocuments(String query, String tenantId) {
        log.info("Searching documents with query: {} for tenant: {}", query, tenantId);
        return documentSearchRepository.findByTenantIdAndFileNameContainingOrContentContaining(tenantId, query, query);
    }

    /**
     * Search documents by file name only (tenant-aware)
     */
    public List<DocumentSearchIndex> searchByFileName(String fileName, String tenantId) {
        log.info("Searching documents by file name: {} for tenant: {}", fileName, tenantId);
        return documentSearchRepository.findByTenantIdAndFileNameContaining(tenantId, fileName);
    }

    /**
     * Search documents by content only (tenant-aware)
     */
    public List<DocumentSearchIndex> searchByContent(String content, String tenantId) {
        log.info("Searching documents by content: {} for tenant: {}", content, tenantId);
        return documentSearchRepository.findByTenantIdAndContentContaining(tenantId, content);
    }

    /**
     * Search documents by content type (tenant-aware)
     */
    public List<DocumentSearchIndex> searchByContentType(String contentType, String tenantId) {
        log.info("Searching documents by content type: {} for tenant: {}", contentType, tenantId);
        return documentSearchRepository.findByTenantIdAndContentType(tenantId, contentType);
    }

    /**
     * Search documents by file type (tenant-aware)
     */
    public List<DocumentSearchIndex> searchByFileType(String fileType, String tenantId) {
        log.info("Searching documents by file type: {} for tenant: {}", fileType, tenantId);
        return documentSearchRepository.findByTenantIdAndFileType(tenantId, fileType);
    }

    /**
     * Get all documents for a tenant
     */
    public List<DocumentSearchIndex> getAllDocumentsByTenant(String tenantId) {
        log.info("Fetching all documents for tenant: {}", tenantId);
        return documentSearchRepository.findByTenantId(tenantId);
    }

    /**
     * Advanced search with criteria (tenant-aware)
     */
    public List<DocumentSearchIndex> advancedSearch(String fileName, String content, String contentType, String fileType, String tenantId) {
        log.info("Advanced search - tenant: {}, fileName: {}, content: {}, contentType: {}, fileType: {}",
                 tenantId, fileName, content, contentType, fileType);

        Criteria criteria = new Criteria("tenantId").is(tenantId);

        if (fileName != null && !fileName.isEmpty()) {
            criteria = criteria.and("fileName").contains(fileName);
        }

        if (content != null && !content.isEmpty()) {
            criteria = criteria.and("content").contains(content);
        }

        if (contentType != null && !contentType.isEmpty()) {
            criteria = criteria.and("contentType").is(contentType);
        }

        if (fileType != null && !fileType.isEmpty()) {
            criteria = criteria.and("fileType").is(fileType);
        }

        Query query = new CriteriaQuery(criteria);
        SearchHits<DocumentSearchIndex> searchHits = elasticsearchOperations.search(query, DocumentSearchIndex.class);

        return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    /**
     * Get document by ID (tenant-aware for security)
     */
    public DocumentSearchIndex getDocumentById(String id, String tenantId) {
        log.info("Fetching document by ID: {} for tenant: {}", id, tenantId);
        DocumentSearchIndex doc = documentSearchRepository.findById(id).orElse(null);
        // Verify tenant ownership
        if (doc != null && !doc.getTenantId().equals(tenantId)) {
            log.warn("Tenant {} attempted to access document {} owned by tenant {}", tenantId, id, doc.getTenantId());
            return null; // Return null if tenant doesn't match
        }
        return doc;
    }

    /**
     * Delete document from Elasticsearch (tenant-aware)
     */
    public void deleteDocument(String id, String tenantId) {
        log.info("Deleting document from Elasticsearch: {} for tenant: {}", id, tenantId);
        DocumentSearchIndex doc = documentSearchRepository.findById(id).orElse(null);
        if (doc != null && doc.getTenantId().equals(tenantId)) {
            documentSearchRepository.deleteById(id);
        } else {
            log.warn("Cannot delete document {} - tenant mismatch or not found", id);
        }
    }
}
