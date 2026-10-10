package com.rtcodeeditor.backend.websocket;

import com.rtcodeeditor.backend.model.AwarenessUpdate;
import com.rtcodeeditor.backend.model.DocumentUpdate;
import com.rtcodeeditor.backend.service.DocumentService;
import java.util.Base64;
import java.util.regex.Pattern;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class CollaborationHandler {

    private static final Pattern DOCUMENT_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,128}");
    private static final int MAX_UPDATE_LENGTH = 1_400_000;

    private final DocumentService documentService;
    private final SimpMessageSendingOperations messagingTemplate;

    public CollaborationHandler(DocumentService documentService, SimpMessageSendingOperations messagingTemplate) {
        this.documentService = documentService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/document.update")
    public void handleDocumentUpdate(@Payload DocumentUpdate update) {
        if (update == null || !isValidDocumentId(update.documentId()) || !isValidUpdate(update.update())) {
            return;
        }

        byte[] stateUpdate;
        try {
            stateUpdate = Base64.getDecoder().decode(update.update());
        } catch (IllegalArgumentException exception) {
            return;
        }
        if (stateUpdate.length == 0 || stateUpdate.length > MAX_UPDATE_LENGTH) {
            return;
        }

        documentService.saveDocumentUpdate(update.documentId(), stateUpdate);
        messagingTemplate.convertAndSend("/topic/document/" + update.documentId(), update);
    }

    @MessageMapping("/document.load")
    @SendToUser(value = "/queue/document/state", broadcast = false)
    public DocumentUpdate handleDocumentLoad(@Payload DocumentUpdate request) {
        if (request == null || !isValidDocumentId(request.documentId())) {
            return null;
        }

        byte[] state = documentService.getDocumentState(request.documentId());
        return new DocumentUpdate(request.documentId(), Base64.getEncoder().encodeToString(state));
    }

    @MessageMapping("/document.awareness")
    public void handleAwarenessUpdate(@Payload AwarenessUpdate update) {
        if (update == null || !isValidDocumentId(update.documentId()) || !isValidUpdate(update.update())) {
            return;
        }

        try {
            byte[] awareness = Base64.getDecoder().decode(update.update());
            if (awareness.length == 0 || awareness.length > MAX_UPDATE_LENGTH) {
                return;
            }
        } catch (IllegalArgumentException exception) {
            return;
        }

        messagingTemplate.convertAndSend("/topic/document/" + update.documentId() + "/awareness", update);
    }

    private boolean isValidUpdate(String update) {
        return update != null && !update.isBlank() && update.length() <= MAX_UPDATE_LENGTH;
    }

    private boolean isValidDocumentId(String documentId) {
        return documentId != null && DOCUMENT_ID_PATTERN.matcher(documentId).matches();
    }
}
