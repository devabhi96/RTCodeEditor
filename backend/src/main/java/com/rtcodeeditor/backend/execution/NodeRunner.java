package com.rtcodeeditor.backend.execution;

import java.util.Arrays;
import java.util.List;

/**
 * Language runner for Node.js.
 * The orchestrator will write the code to a file named {@code script.js}.
 */
public class NodeRunner implements LanguageRunner {
    @Override
    public List<String> prepareCommand(String code) {
        // Run the Node.js script.
        return Arrays.asList("sh", "-c", "node script.js");
    }

    @Override
    public String getImageName() {
        return "node:20-slim";
    }
}