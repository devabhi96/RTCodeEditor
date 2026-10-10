package com.rtcodeeditor.backend.execution;

/**
 * Interface for language-specific runners that handle compilation (if needed) and execution of code snippets.
 */
public interface LanguageRunner {
    /**
     * Prepares the command to run in the container.
     *
     * @param code the user code to execute
     * @return the command and arguments as a list of strings
     */
    java.util.List<String> prepareCommand(String code);

    /**
     * Returns the Docker image name to use for this language.
     *
     * @return the Docker image name
     */
    String getImageName();
}