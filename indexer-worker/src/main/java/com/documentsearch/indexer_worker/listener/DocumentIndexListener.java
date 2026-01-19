package com.documentsearch.indexer_worker.listener;

import com.documentsearch.indexer_worker.dto.DocumentIndexMessage;
import com.documentsearch.indexer_worker.service.DocumentIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentIndexListener {

    private final DocumentIndexingService documentIndexingService;

    @RabbitListener(queues = "document.index.queue")
    public void handleDocumentIndexMessage(DocumentIndexMessage message) {
        log.info("Received message for document ID: {} - File: {}",
                message.getDocumentId(), message.getFileName());

        try {
            documentIndexingService.indexDocument(message.getDocumentId(), message.getFilePath());
            log.info("Successfully processed message for document ID: {}", message.getDocumentId());
        } catch (Exception e) {
            log.error("Failed to process message for document ID: {}", message.getDocumentId(), e);
            // In production, you might want to send to a dead letter queue here
        }
    }
}

