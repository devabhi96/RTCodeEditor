package com.rtcodeeditor.backend.websocket;

import com.rtcodeeditor.backend.execution.ExecutionRequest;
import com.rtcodeeditor.backend.execution.ExecutionResponse;
import com.rtcodeeditor.backend.execution.ExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller handling code execution requests.
 */
@Controller
public class ExecutionController {

    private final ExecutionService executionService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    public ExecutionController(ExecutionService executionService, SimpMessagingTemplate simpMessagingTemplate) {
        this.executionService = executionService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    /**
     * Receives an execution request, executes the code in a secure container,
     * and sends the output to the requesting user.
     *
     * @param request the execution request containing document ID, language, and code
     * @param headerAccessor header accessor to get the user information
     */
    @MessageMapping("/app/execute")
    public void handleExecutionRequest(ExecutionRequest request, SimpMessageHeaderAccessor headerAccessor) {
        // Validate document ID format (same as in CollaborationHandler)
        if (request == null || request.getDocumentId() == null ||
                !isValidDocumentId(request.getDocumentId())) {
            sendErrorResponse(headerAccessor, "Invalid document ID");
            return;
        }
        // Optionally validate code size to prevent excessively large inputs
        if (request.getCode() != null && request.getCode().length() > 100_000) { // 100KB limit
            sendErrorResponse(headerAccessor, "Code exceeds maximum allowed size (100 KB)");
            return;
        }

        // Delegate to the execution service
        ExecutionResponse response = executionService.execute(request);
        // Send the response to the user
        simpMessagingTemplate.convertAndSendToUser(getUserName(headerAccessor), "/queue/execution/output", response);
    }

    private void sendErrorResponse(SimpMessageHeaderAccessor headerAccessor, String errorMessage) {
        ExecutionResponse response = new ExecutionResponse();
        // Document ID may not be available in error case; we leave it empty.
        response.setStderr(errorMessage);
        response.setExitCode(1);
        response.setSuccess(false);
        simpMessagingTemplate.convertAndSendToUser(getUserName(headerAccessor), "/queue/execution/output", response);
    }

    private String getUserName(SimpMessageHeaderAccessor headerAccessor) {
        // Assuming the user principal is set in the header attributes after authentication
        // In our WebSocketConfig, we don't explicitly set a user, but we rely on Spring Security?
        // For simplicity, we can use the session ID as a fallback.
        // However, we need to get the authenticated user.
        // Since we are using JWT, we might have stored the user in the session attributes.
        // We'll try to get the user from the header attributes.
        Object userObj = headerAccessor.getSessionAttributes().get("user");
        if (userObj != null) {
            return userObj.toString();
        }
        // Fallback to the session ID if no user attribute is set
        return headerAccessor.getSessionId();
    }

    /**
     * Validates a document ID using the same pattern as in CollaborationHandler.
     * Allow 1-128 characters: letters, numbers, hyphens, underscores.
     *
     * @param documentId the document ID to validate
     * @return true if valid
     */
    private boolean isValidDocumentId(String documentId) {
        if (documentId == null) {
            return false;
        }
        return documentId.matches("[A-Za-z0-9_-]{1,128}");
    }
}