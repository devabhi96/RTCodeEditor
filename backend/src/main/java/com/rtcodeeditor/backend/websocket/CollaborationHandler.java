package com.rtcodeeditor.backend.websocket;

import com.rtcodeeditor.backend.service.DocumentService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class CollaborationHandler {

    private final DocumentService documentService;
    private final SimpMessagingTemplate messagingTemplate;

    public CollaborationHandler(DocumentService documentService, SimpMessagingTemplate messagingTemplate) {
        this.documentService = documentService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/document.update")
    public void handleDocumentUpdate(Map<String, Object> message) {
        Object rawDocumentId = message.get("documentId");
        Object rawContent = message.get("content");
        if (!(rawDocumentId instanceof String documentId) || documentId.isBlank()
                || !(rawContent instanceof String content)) {
            return;
        }

        documentService.saveDocumentContent(documentId, content);
        messagingTemplate.convertAndSend("/topic/document/" + documentId, message);
    }
}
