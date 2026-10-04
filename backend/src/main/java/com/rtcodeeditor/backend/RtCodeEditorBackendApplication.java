package com.rtcodeeditor.backend;

// These are "import statements" - they tell our code where to find tools we need
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// This annotation tells Spring Boot: "This is the main class of our application"
// Think of it like putting a "Start Here" sign on this file
@SpringBootApplication
public class RtCodeEditorBackendApplication {

    // This is the main method - the entry point where our program starts running
    // When you run the application, Java looks for this method first
    public static void main(String[] args) {
        // This line tells Spring Boot: "Please start up our application"
        // Spring Boot will then:
        // 1. Look at all our @SpringBootApplication annotated classes (this one)
        // 2. Automatically configure everything we need
        // 3. Start an embedded web server (usually on port 8080)
        // 4. Make our application ready to receive requests
        SpringApplication.run(RtCodeEditorBackendApplication.class, args);
    }
}