package com.documentsearch.document_management_service.service;

import com.documentsearch.document_management_service.config.RabbitMQConfig;
import com.documentsearch.document_management_service.dto.DocumentIndexMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RabbitMQPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void publishDocumentIndexMessage(DocumentIndexMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DOCUMENT_EXCHANGE,
                    RabbitMQConfig.DOCUMENT_INDEX_ROUTING_KEY,
                    message
            );
            log.info("Published document index message for document ID: {}", message.getDocumentId());
        } catch (Exception e) {
            log.error("Error publishing message to RabbitMQ", e);
            throw new RuntimeException("Failed to publish message", e);
        }
    }
}

