package com.rtcodeeditor.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "document_updates")
public class DocumentState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", length = 128, nullable = false)
    private String documentId;

    @Column(name = "state_update", nullable = false, columnDefinition = "BYTEA")
    private byte[] stateUpdate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DocumentState() {
    }

    public DocumentState(String documentId, byte[] stateUpdate) {
        this.documentId = documentId;
        this.stateUpdate = stateUpdate.clone();
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public byte[] getStateUpdate() {
        return stateUpdate.clone();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
