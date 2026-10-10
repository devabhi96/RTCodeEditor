package com.rtcodeeditor.backend.execution;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Service responsible for executing code snippets in secure Docker containers.
 */
@Service
public class ExecutionService {

    private final DockerOrchestrator orchestrator;
    private final long executionTimeoutMs;

    public ExecutionService(@Value("${execution.timeout.ms:5000}") long executionTimeoutMs) {
        this.orchestrator = new DockerOrchestrator();
        this.executionTimeoutMs = executionTimeoutMs;
    }

    @PostConstruct
    public void init() {
        // Initialization if needed
    }

    @PreDestroy
    public void cleanup() {
        orchestrator.close();
    }

    /**
     * Executes the code provided in the request.
     *
     * @param request the execution request containing document ID, language, and code
     * @return the execution response with output and status
     */
    public ExecutionResponse execute(ExecutionRequest request) {
        if (request == null) {
            return new ExecutionResponse();
        }
        String documentId = request.getDocumentId();
        String language = request.getLanguage();
        String code = request.getCode();

        // Validate inputs
        if (documentId == null || language == null || code == null) {
            ExecutionResponse response = new ExecutionResponse();
            response.setDocumentId(documentId);
            response.setStderr("Invalid request: missing documentId, language, or code");
            response.setExitCode(1);
            response.setSuccess(false);
            return response;
        }

        // Delegate to the orchestrator
        return orchestrator.execute(documentId, language, code, executionTimeoutMs);
    }
}