package com.rtcodeeditor.backend.execution;

public class ExecutionRequest {
    private String documentId;
    private String language;
    private String code;

    public ExecutionRequest() {
    }

    public ExecutionRequest(String documentId, String language, String code) {
        this.documentId = documentId;
        this.language = language;
        this.code = code;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}