package com.documentsearch.document_search_service.repository;

import com.documentsearch.document_search_service.model.DocumentSearchIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentSearchRepository extends ElasticsearchRepository<DocumentSearchIndex, String> {

    // Search by tenant ID
    List<DocumentSearchIndex> findByTenantId(String tenantId);

    // Search by file name and tenant
    List<DocumentSearchIndex> findByFileNameContainingAndTenantId(String fileName, String tenantId);

    // Search by content and tenant
    List<DocumentSearchIndex> findByContentContainingAndTenantId(String content, String tenantId);

    // Hybrid search: filename or content, filtered by tenant
    List<DocumentSearchIndex> findByTenantIdAndFileNameContainingOrTenantIdAndContentContaining(
            String tenantId1, String fileName, String tenantId2, String content);
}

