package com.rtcodeeditor.backend.model;

import java.time.LocalDateTime;

/**
 * Simple document model for Phase 1
 * In later phases, this would be a JPA entity with persistence
 */
public class Document {
    private String id;
    private String content;
    private LocalDateTime lastUpdatedAt;
    private String lastUpdatedBy;

    // Constructors
    public Document() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public Document(String id, String content) {
        this.id = id;
        this.content = content;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }
}
