package com.rtcodeeditor.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rtcodeeditor.backend.repository.DocumentStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DocumentServiceIntegrationTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentStateRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearIntegrationDocument() {
        jdbcTemplate.update("DELETE FROM document_updates WHERE document_id = ?", "integration-doc");
    }

    @Test
    void appendsUpdatesAndReconstructsThePersistedDocumentBundle() {
        documentService.saveDocumentUpdate("integration-doc", new byte[]{1, 2, 3});
        documentService.saveDocumentUpdate("integration-doc", new byte[]{4, 5});

        assertThat(documentService.getDocumentState("integration-doc"))
                .containsExactly(3, 0, 0, 0, 1, 2, 3, 2, 0, 0, 0, 4, 5);
        assertThat(repository.findAllByDocumentIdOrderByIdAsc("integration-doc")).hasSize(2);
    }
}
