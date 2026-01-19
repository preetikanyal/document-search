package com.documentsearch.document_search_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {
    private String id;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String contentSnippet; // First 200 characters of content

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime uploadedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime indexedAt;

    private String status;
    private String filePath;
    private String tenantId;
    private Double score; // Relevance score
}
