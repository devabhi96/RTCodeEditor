package com.rtcodeeditor.backend.service;

import com.rtcodeeditor.backend.model.Document;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory document service for Phase 1
 * In later phases, this would use a database repository
 */
@Service
public class DocumentService {
    
    // In-memory storage for documents
    // Key: documentId, Value: Document content
    private final Map<String, String> documents = new ConcurrentHashMap<>();
    
    /**
     * Get document content by ID
     * @param documentId The ID of the document
     * @return The document content, or empty string if not found
     */
    public String getDocumentContent(String documentId) {
        return documents.getOrDefault(documentId, "");
    }
    
    /**
     * Save or update document content
     * @param documentId The ID of the document
     * @param content The content to save
     */
    public void saveDocumentContent(String documentId, String content) {
        documents.put(documentId, content);
    }
    
    /**
     * Check if document exists
     * @param documentId The ID of the document
     * @return true if document exists
     */
    public boolean documentExists(String documentId) {
        return documents.containsKey(documentId);
    }
}
