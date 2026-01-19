package com.documentsearch.document_search_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private List<SearchResultDTO> results;
    private int totalResults;
    private String query;
    private String tenantId;
    private String message;
    private long searchTimeMs;

    public SearchResponse(List<SearchResultDTO> results, String query, String tenantId, long searchTimeMs) {
        this.results = results;
        this.totalResults = results.size();
        this.query = query;
        this.tenantId = tenantId;
        this.searchTimeMs = searchTimeMs;
        this.message = totalResults > 0
            ? "Found " + totalResults + " document(s) matching your query"
            : "No documents found matching your query";
    }
}

