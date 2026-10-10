package com.rtcodeeditor.backend.execution;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates Docker containers for secure code execution using Docker CLI.
 * Handles image pulling, container creation with security hardening and resource limits,
 * and streaming of execution output.
 */
public class DockerOrchestrator {

    private static final long DEFAULT_TIMEOUT_MS = 5000; // 5 seconds as per PRD
    private static final long MEMORY_LIMIT_BYTES = 256L * 1024 * 1024; // 256 MB
    private static final long CPU_SHARES = 500;

    public DockerOrchestrator() {
    }

    /**
     * Executes the given code in a secure container and returns the result.
     *
     * @param documentId the ID of the document (for tracking)
     * @param language   the programming language (java, python, nodejs)
     * @param code       the source code to execute
     * @param timeoutMillis execution timeout in milliseconds
     * @return execution response containing stdout, stderr, exit code, and success flag
     */
    public ExecutionResponse execute(String documentId, String language, String code, long timeoutMillis) {
        // Select the appropriate language runner to get the image name and command.
        LanguageRunner runner = getLanguageRunner(language);
        String imageName = runner.getImageName();
        List<String> command = runner.prepareCommand(code);

        // Create a temporary directory on the host to hold source files.
        Path hostTempDir = null;
        String sourceFileName = "";
        try {
            hostTempDir = Files.createTempDirectory("rtcode-exec-");
            // Determine the source file name based on language.
            if (language.equalsIgnoreCase("java")) {
                sourceFileName = "Main.java";
            } else if (language.equalsIgnoreCase("python") || language.equalsIgnoreCase("nodejs") || language.equalsIgnoreCase("node")) {
                sourceFileName = "script." + (language.equalsIgnoreCase("python") ? "py" : "js");
            } else {
                // fallback
                sourceFileName = "code.txt";
            }
            Path sourceFile = hostTempDir.resolve(sourceFileName);
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            // Generate a unique container name.
            String containerName = "rtcode-exec-" + UUID.randomUUID().toString().substring(0, 8);

            // Build the docker create command.
            List<String> createCmd = new ArrayList<>();
            createCmd.add("docker");
            createCmd.add("create");
            createCmd.add("--name");
            createCmd.add(containerName);
            // Resource limits.
            createCmd.add("--memory");
            createCmd.add(String.valueOf(MEMORY_LIMIT_BYTES));
            createCmd.add("--memory-swap");
            createCmd.add("0"); // Disable swap
            createCmd.add("--cpu-shares");
            createCmd.add(String.valueOf(CPU_SHARES));
            // Security options.
            createCmd.add("--read-only"); // Read-only root filesystem.
            createCmd.add("--network"); // Network mode.
            createCmd.add("none");
            createCmd.add("--cap-drop");
            createCmd.add("ALL");
            createCmd.add("--cap-add");
            createCmd.add("SETUID");
            createCmd.add("--cap-add");
            createCmd.add("SETGID");
            createCmd.add("--cap-add");
            createCmd.add("NET_RAW");
            // Working directory.
            createCmd.add("--workdir");
            createCmd.add("/tmp");
            // Image name.
            createCmd.add(imageName);
            // Command.
            createCmd.addAll(command);

            // Create the container.
            Process createProcess = new ProcessBuilder(createCmd).start();
            int createExitCode = createProcess.waitFor();
            if (createExitCode != 0) {
                throw new IOException("Failed to create container: " + readError(createProcess.getErrorStream()));
            }

            // Get the container ID from the output of docker create.
            String containerId = readOutput(createProcess.getInputStream()).trim();
            if (containerId.isEmpty()) {
                throw new IOException("Failed to get container ID from docker create output");
            }

            try {
                // Copy the source file into the container at /tmp/<sourceFileName>
                List<String> copyCmd = new ArrayList<>();
                copyCmd.add("docker");
                copyCmd.add("cp");
                copyCmd.add(sourceFile.toString());
                copyCmd.add(containerId + ":/tmp/" + sourceFileName);
                Process copyProcess = new ProcessBuilder(copyCmd).start();
                int copyExitCode = copyProcess.waitFor();
                if (copyExitCode != 0) {
                    throw new IOException("Failed to copy file to container: " + readError(copyProcess.getErrorStream()));
                }

                // Start the container.
                List<String> startCmd = new ArrayList<>();
                startCmd.add("docker");
                startCmd.add("start");
                startCmd.add(containerId);
                Process startProcess = new ProcessBuilder(startCmd).start();
                int startExitCode = startProcess.waitFor();
                if (startExitCode != 0) {
                    throw new IOException("Failed to start container: " + readError(startProcess.getErrorStream()));
                }

                // Wait for the container to finish with a timeout.
                long startTime = System.currentTimeMillis();
                boolean containerFinished = false;
                Integer exitCode = null;
                boolean timedOut = false;
                while (System.currentTimeMillis() - startTime < timeoutMillis) {
                    // Check if the container is still running.
                    List<String> psCmd = new ArrayList<>();
                    psCmd.add("docker");
                    psCmd.add("ps");
                    psCmd.add("--filter");
                    psCmd.add("id=" + containerId);
                    psCmd.add("--format");
                    psCmd.add("{{.Status}}");
                    Process psProcess = new ProcessBuilder(psCmd).start();
                    int psExitCode = psProcess.waitFor();
                    if (psExitCode == 0) {
                        String status = readOutput(psProcess.getInputStream()).trim();
                        if (status.isEmpty() || !status.startsWith("Up")) {
                            // Container is not running.
                            containerFinished = true;
                            // Get the exit code.
                            List<String> inspectCmd = new ArrayList<>();
                            inspectCmd.add("docker");
                            inspectCmd.add("inspect");
                            inspectCmd.add("--format");
                            inspectCmd.add("{{.State.ExitCode}}");
                            inspectCmd.add(containerId);
                            Process inspectProcess = new ProcessBuilder(inspectCmd).start();
                            int inspectExitCode = inspectProcess.waitFor();
                            if (inspectExitCode == 0) {
                                String exitCodeStr = readOutput(inspectProcess.getInputStream()).trim();
                                try {
                                    exitCode = Integer.parseInt(exitCodeStr);
                                } catch (NumberFormatException e) {
                                    exitCode = 1;
                                }
                            } else {
                                exitCode = 1;
                            }
                            break;
                        }
                    }
                    // Sleep for a short interval to avoid busy waiting.
                    Thread.sleep(100);
                }

                if (!containerFinished) {
                    // Timeout occurred.
                    List<String> killCmd = new ArrayList<>();
                    killCmd.add("docker");
                    killCmd.add("kill");
                    killCmd.add(containerId);
                    Process killProcess = new ProcessBuilder(killCmd).start();
                    int killExitCode = killProcess.waitFor();
                    // We don't care about the exit code of kill.
                    exitCode = 124; // Standard timeout exit code.
                    timedOut = true;
                    containerFinished = true;
                }

                // Get the logs.
                List<String> logsCmd = new ArrayList<>();
                logsCmd.add("docker");
                logsCmd.add("logs");
                logsCmd.add(containerId);
                Process logsProcess = new ProcessBuilder(logsCmd).start();
                int logsExitCode = logsProcess.waitFor();
                String stdout = readOutput(logsProcess.getInputStream());
                String stderr = readError(logsProcess.getErrorStream());

                // Build the response.
                ExecutionResponse response = new ExecutionResponse();
                response.setDocumentId(documentId);
                response.setStdout(stdout);
                response.setStderr(stderr);
                response.setExitCode(exitCode != null ? exitCode : 1);
                boolean success = (exitCode != null && exitCode == 0);
                response.setSuccess(success);
                response.setTimestamp(java.time.Instant.now());

                // If we had a timeout, append a message to stderr.
                if (timedOut) {
                    response.setStderr(response.getStderr() + "Execution timed out after " + timeoutMillis + " ms.");
                    response.setSuccess(false);
                }

                return response;
            } finally {
                // Clean up: remove the container.
                List<String> rmCmd = new ArrayList<>();
                rmCmd.add("docker");
                rmCmd.add("rm");
                rmCmd.add("-f");
                rmCmd.add(containerId);
                try {
                    Process rmProcess = new ProcessBuilder(rmCmd).start();
                    rmProcess.waitFor();
                } catch (IOException | InterruptedException e) {
                    // Ignore cleanup errors.
                }
                // Delete the host temp directory.
                if (hostTempDir != null) {
                    try {
                        Files.walk(hostTempDir)
                                .sorted(Comparator.reverseOrder())
                                .forEach(path -> {
                                    try {
                                        Files.deleteIfExists(path);
                                    } catch (IOException e) {
                                        // Ignore deletion errors
                                    }
                                });
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            // If we fail to set up the temporary directory, return an error.
            ExecutionResponse response = new ExecutionResponse();
            response.setDocumentId(documentId);
            response.setStderr("Failed to prepare execution environment: " + e.getMessage());
            response.setExitCode(1);
            response.setSuccess(false);
            response.setTimestamp(java.time.Instant.now());
            // Clean up if hostTempDir was created.
            if (hostTempDir != null) {
                try {
                    Files.walk(hostTempDir)
                            .sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ex) {
                                    // Ignore
                                }
                            });
                } catch (IOException ex) {
                    // Ignore
                }
            }
            return response;
        }
    }

    /**
     * Reads the full output from an InputStream and returns it as a string.
     *
     * @param inputStream the input stream to read
     * @return the output as a string
     * @throws IOException if an I/O error occurs
     */
    private String readOutput(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    /**
     * Reads the full error from an InputStream and returns it as a string.
     *
     * @param inputStream the input stream to read
     * @return the error as a string
     * @throws IOException if an I/O error occurs
     */
    private String readError(InputStream inputStream) throws IOException {
        return readOutput(inputStream);
    }

    /**
     * Pulls the Docker image if not present locally.
     *
     * @param imageName the image name to pull
     */
    private void pullImageIfNeeded(String imageName) {
        try {
            // Inspect the image to see if it exists locally.
            List<String> inspectCmd = new ArrayList<>();
            inspectCmd.add("docker");
            inspectCmd.add("inspect");
            inspectCmd.add("image");
            inspectCmd.add(imageName);
            Process inspectProcess = new ProcessBuilder(inspectCmd).start();
            int inspectExitCode = inspectProcess.waitFor();
            if (inspectExitCode == 0) {
                // Image exists.
                return;
            }
            // Image not found, pull it.
            List<String> pullCmd = new ArrayList<>();
            pullCmd.add("docker");
            pullCmd.add("pull");
            pullCmd.add(imageName);
            Process pullProcess = new ProcessBuilder(pullCmd).start();
            int pullExitCode = pullProcess.waitFor();
            if (pullExitCode != 0) {
                throw new IOException("Failed to pull image: " + readError(pullProcess.getErrorStream()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error checking/pulling image: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the appropriate LanguageRunner for the given language.
     *
     * @param language the language identifier (case-insensitive)
     * @return a LanguageRunner instance
     */
    private LanguageRunner getLanguageRunner(String language) {
        switch (language.toLowerCase()) {
            case "java":
                return new JavaRunner();
            case "python":
                return new PythonRunner();
            case "nodejs":
            case "node":
                return new NodeRunner();
            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    /**
     * Shuts down the Docker client (no-op for CLI-based implementation).
     */
    public void close() {
    }

    /**
     * Language runner strategy interface.
     */
    private interface LanguageRunner {
        String getImageName();
        List<String> prepareCommand(String code);
    }

    /**
     * Java language runner.
     */
    private class JavaRunner implements LanguageRunner {
        @Override
        public String getImageName() {
            return "openjdk:17-slim";
        }

        @Override
        public List<String> prepareCommand(String code) {
            // The code is already copied to /tmp/Main.java, so we just need to compile and run.
            return List.of("sh", "-c", "javac Main.java && java Main");
        }
    }

    /**
     * Python language runner.
     */
    private class PythonRunner implements LanguageRunner {
        @Override
        public String getImageName() {
            return "python:3.12-slim";
        }

        @Override
        public List<String> prepareCommand(String code) {
            return List.of("sh", "-c", "python3 script.py");
        }
    }

    /**
     * Node.js language runner.
     */
    private class NodeRunner implements LanguageRunner {
        @Override
        public String getImageName() {
            return "node:20-slim";
        }

        @Override
        public List<String> prepareCommand(String code) {
            return List.of("sh", "-c", "node script.js");
        }
    }
}