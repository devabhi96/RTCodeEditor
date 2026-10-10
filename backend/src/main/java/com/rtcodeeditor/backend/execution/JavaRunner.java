package com.rtcodeeditor.backend.execution;

import java.util.Arrays;
import java.util.List;

/**
 * Language runner for Java.
 * Assumes the code contains a public class named {@code Main} with a {@code main(String[])} method.
 * The orchestrator will write the code to a file named {@code Main.java}.
 */
public class JavaRunner implements LanguageRunner {
    @Override
    public List<String> prepareCommand(String code) {
        // Compile and run the Java code.
        return Arrays.asList("sh", "-c", "javac Main.java && java Main");
    }

    @Override
    public String getImageName() {
        // Use an openjdk image that includes the javac and java commands.
        return "openjdk:17-slim";
    }
}