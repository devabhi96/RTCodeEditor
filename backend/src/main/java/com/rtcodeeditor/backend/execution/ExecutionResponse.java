package com.rtcodeeditor.backend.execution;

import java.time.Instant;

public class ExecutionResponse {
    private String documentId;
    private String stdout;
    private String stderr;
    private int exitCode;
    private boolean success;
    private Instant timestamp;

    public ExecutionResponse() {
        this.timestamp = Instant.now();
    }

    public ExecutionResponse(String documentId, String stdout, String stderr, int exitCode, boolean success) {
        this.documentId = documentId;
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitCode = exitCode;
        this.success = success;
        this.timestamp = Instant.now();
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}