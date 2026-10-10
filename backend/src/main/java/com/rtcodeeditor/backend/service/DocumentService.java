package com.rtcodeeditor.backend.service;

import com.rtcodeeditor.backend.model.DocumentState;
import com.rtcodeeditor.backend.repository.DocumentStateRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService {

    private final DocumentStateRepository documentStateRepository;

    public DocumentService(DocumentStateRepository documentStateRepository) {
        this.documentStateRepository = documentStateRepository;
    }

    @Transactional(readOnly = true)
    public byte[] getDocumentState(String documentId) {
        List<DocumentState> updates = documentStateRepository.findAllByDocumentIdOrderByIdAsc(documentId);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (DocumentState update : updates) {
            byte[] bytes = update.getStateUpdate();
            output.write(bytes.length);
            output.write(bytes.length >>> 8);
            output.write(bytes.length >>> 16);
            output.write(bytes.length >>> 24);
            try {
                output.write(bytes);
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }
        return output.toByteArray();
    }

    @Transactional
    public void saveDocumentUpdate(String documentId, byte[] update) {
        documentStateRepository.appendUpdate(documentId, update);
    }
}
