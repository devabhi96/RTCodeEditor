package com.rtcodeeditor.backend.execution;

import java.util.Arrays;
import java.util.List;

/**
 * Language runner for Python 3.
 * The orchestrator will write the code to a file named {@code script.py}.
 */
public class PythonRunner implements LanguageRunner {
    @Override
    public List<String> prepareCommand(String code) {
        // Run the Python script.
        return Arrays.asList("sh", "-c", "python3 script.py");
    }

    @Override
    public String getImageName() {
        return "python:3.12-slim";
    }
}