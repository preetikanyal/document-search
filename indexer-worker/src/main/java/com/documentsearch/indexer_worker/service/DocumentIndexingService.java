package com.documentsearch.indexer_worker.service;

import com.documentsearch.indexer_worker.elasticsearch.DocumentSearchIndex;
import com.documentsearch.indexer_worker.elasticsearch.DocumentSearchRepository;
import com.documentsearch.indexer_worker.entity.Document;
import com.documentsearch.indexer_worker.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIndexingService {

    private final DocumentRepository documentRepository;
    private final DocumentSearchRepository documentSearchRepository;
    private final Tika tika = new Tika();

    @Transactional
    public void indexDocument(Long documentId, String filePath) {
        log.info("Starting indexing for document ID: {} at path: {}", documentId, filePath);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + documentId));

        try {
            // Update status to PROCESSING
            document.setStatus("PROCESSING");
            documentRepository.save(document);

            // Extract text from document using Apache Tika
            File file = new File(filePath);
            if (!file.exists()) {
                throw new RuntimeException("File not found at path: " + filePath);
            }

            log.info("Extracting text from file: {}", file.getName());
            String extractedText = tika.parseToString(file);

            log.info("Extracted {} characters from document {}", extractedText.length(), documentId);

            // Update document status (NO extracted text stored in MySQL)
            document.setStatus("INDEXED");
            document.setIndexedAt(LocalDateTime.now());
            documentRepository.save(document);

            // Index to Elasticsearch with both metadata and content
            log.info("Indexing document to Elasticsearch: {}", documentId);
            DocumentSearchIndex searchIndex = new DocumentSearchIndex();
            searchIndex.setId(documentId.toString());
            searchIndex.setFileName(document.getFileName());
            searchIndex.setContentType(document.getContentType());
            searchIndex.setFileType(document.getFileType());
            searchIndex.setFileSize(document.getFileSize());
            searchIndex.setTenantId(document.getTenantId());
            searchIndex.setContent(extractedText); // Content ONLY stored in Elasticsearch
            searchIndex.setUploadedAt(document.getUploadedAt());
            searchIndex.setIndexedAt(document.getIndexedAt());
            searchIndex.setStatus(document.getStatus());
            searchIndex.setFilePath(document.getFilePath());

            documentSearchRepository.save(searchIndex);
            log.info("Successfully indexed document to Elasticsearch: {} for tenant: {}", documentId, document.getTenantId());

            log.info("Successfully indexed document ID: {}", documentId);

        } catch (Exception e) {
            log.error("Error indexing document ID: {}", documentId, e);
            document.setStatus("FAILED");
            documentRepository.save(document);
            throw new RuntimeException("Failed to index document", e);
        }
    }
}
