package com.rtcodeeditor.backend.websocket;

import com.rtcodeeditor.backend.model.AwarenessUpdate;
import com.rtcodeeditor.backend.model.DocumentUpdate;
import com.rtcodeeditor.backend.model.DocumentState;
import com.rtcodeeditor.backend.service.DocumentService;
import com.rtcodeeditor.backend.repository.DocumentStateRepository;
import java.util.List;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CollaborationHandlerTest {

    private DocumentStateRepository repository;
    private SimpMessageSendingOperations messagingTemplate;
    private CollaborationHandler handler;

    @BeforeEach
    void setUp() {
        repository = mock(DocumentStateRepository.class);
        messagingTemplate = mock(SimpMessageSendingOperations.class);
        handler = new CollaborationHandler(new DocumentService(repository), messagingTemplate);
    }

    @Test
    void persistsAndBroadcastsValidUpdate() {
        String encoded = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        DocumentUpdate update = new DocumentUpdate("doc-1", encoded);

        handler.handleDocumentUpdate(update);

        verify(repository).appendUpdate("doc-1", new byte[]{1, 2, 3});
        verify(messagingTemplate).convertAndSend("/topic/document/doc-1", update);
    }

    @Test
    void ignoresInvalidDocumentUpdates() {
        handler.handleDocumentUpdate(new DocumentUpdate(" ", "AQ=="));
        handler.handleDocumentUpdate(new DocumentUpdate("x".repeat(129), "AQ=="));
        handler.handleDocumentUpdate(new DocumentUpdate("doc-1", "not-base64"));
        handler.handleDocumentUpdate(new DocumentUpdate("doc-1", ""));
        handler.handleDocumentUpdate(null);

        verifyNoInteractions(repository, messagingTemplate);
    }

    @Test
    void loadsPersistedUpdatesForTheRequestingSession() {
        when(repository.findAllByDocumentIdOrderByIdAsc("doc-1")).thenReturn(List.of(
                new DocumentState("doc-1", new byte[]{1, 2}),
                new DocumentState("doc-1", new byte[]{3})));

        String encodedState = Base64.getEncoder().encodeToString(new byte[]{2, 0, 0, 0, 1, 2, 1, 0, 0, 0, 3});
        org.junit.jupiter.api.Assertions.assertEquals(
                new DocumentUpdate("doc-1", encodedState),
                handler.handleDocumentLoad(new DocumentUpdate("doc-1", null)));
    }

    @Test
    void rejectsInvalidLoadRequest() {
        org.junit.jupiter.api.Assertions.assertNull(handler.handleDocumentLoad(new DocumentUpdate(" ", null)));
        org.junit.jupiter.api.Assertions.assertNull(handler.handleDocumentLoad(null));
        verifyNoInteractions(repository, messagingTemplate);
    }

    @Test
    void relaysValidAwarenessWithoutPersistingIt() {
        AwarenessUpdate update = new AwarenessUpdate("doc-1", Base64.getEncoder().encodeToString(new byte[]{4}));

        handler.handleAwarenessUpdate(update);

        verify(messagingTemplate).convertAndSend("/topic/document/doc-1/awareness", update);
        verifyNoInteractions(repository);
    }
}
