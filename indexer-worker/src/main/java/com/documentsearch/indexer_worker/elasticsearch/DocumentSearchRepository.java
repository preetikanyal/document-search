package com.documentsearch.indexer_worker.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentSearchRepository extends ElasticsearchRepository<DocumentSearchIndex, String> {

    // Search by tenant and file name
    List<DocumentSearchIndex> findByTenantIdAndFileNameContaining(String tenantId, String fileName);

    // Search by tenant and content
    List<DocumentSearchIndex> findByTenantIdAndContentContaining(String tenantId, String content);

    // Search by tenant and content type
    List<DocumentSearchIndex> findByTenantIdAndContentType(String tenantId, String contentType);

    // Search by tenant and file type
    List<DocumentSearchIndex> findByTenantIdAndFileType(String tenantId, String fileType);

    // Combined search by tenant and (file name or content)
    List<DocumentSearchIndex> findByTenantIdAndFileNameContainingOrContentContaining(String tenantId, String fileName, String content);

    // Get all documents for a tenant
    List<DocumentSearchIndex> findByTenantId(String tenantId);
}
