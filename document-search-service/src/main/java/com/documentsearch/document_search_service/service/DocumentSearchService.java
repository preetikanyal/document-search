package com.documentsearch.document_search_service.service;

import com.documentsearch.document_search_service.dto.SearchResultDTO;
import com.documentsearch.document_search_service.model.DocumentSearchIndex;
import com.documentsearch.document_search_service.repository.DocumentSearchRepository;
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
     * Hybrid search across metadata and content, filtered by tenant
     */
    public List<SearchResultDTO> hybridSearch(String query, String tenantId) {
        log.info("Performing hybrid search with query: '{}' for tenant: {}", query, tenantId);

        if (query == null || query.trim().isEmpty()) {
            log.warn("Empty query provided, returning all documents for tenant: {}", tenantId);
            return getAllDocumentsForTenant(tenantId);
        }

        // Build criteria for hybrid search
        Criteria criteria = new Criteria("tenantId").is(tenantId)
                .and(new Criteria().or("fileName").contains(query)
                        .or("content").contains(query));

        Query searchQuery = new CriteriaQuery(criteria);
        SearchHits<DocumentSearchIndex> searchHits = elasticsearchOperations.search(
                searchQuery, DocumentSearchIndex.class);

        log.info("Found {} results for query: '{}' and tenant: {}", searchHits.getTotalHits(), query, tenantId);

        return searchHits.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all documents for a tenant (when query is empty)
     */
    private List<SearchResultDTO> getAllDocumentsForTenant(String tenantId) {
        List<DocumentSearchIndex> documents = documentSearchRepository.findByTenantId(tenantId);
        return documents.stream()
                .map(doc -> mapToDTO(doc, null))
                .collect(Collectors.toList());
    }

    /**
     * Map SearchHit to DTO with score
     */
    private SearchResultDTO mapToDTO(SearchHit<DocumentSearchIndex> searchHit) {
        DocumentSearchIndex doc = searchHit.getContent();
        return mapToDTO(doc, searchHit.getScore());
    }

    /**
     * Map DocumentSearchIndex to DTO
     */
    private SearchResultDTO mapToDTO(DocumentSearchIndex doc, Float score) {
        SearchResultDTO dto = new SearchResultDTO();
        dto.setId(doc.getId());
        dto.setFileName(doc.getFileName());
        dto.setContentType(doc.getContentType());
        dto.setFileSize(doc.getFileSize());
        dto.setContentSnippet(truncateContent(doc.getContent(), 200));
        dto.setUploadedAt(doc.getUploadedAt());
        dto.setIndexedAt(doc.getIndexedAt());
        dto.setStatus(doc.getStatus());
        dto.setFilePath(doc.getFilePath());
        dto.setTenantId(doc.getTenantId());
        dto.setScore(score != null ? score.doubleValue() : null);
        return dto;
    }

    /**
     * Truncate content to specified length for snippet
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return null;
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}

