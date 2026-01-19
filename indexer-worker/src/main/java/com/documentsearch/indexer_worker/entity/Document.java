package com.documentsearch.indexer_worker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String contentType;

    @Column
    private String fileType; // pdf, doc, docx, txt, etc.

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String tenantId; // Tenant identifier for multi-tenancy

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column
    private String status; // UPLOADED, PROCESSING, INDEXED, FAILED

    @Column
    private LocalDateTime indexedAt;
}
