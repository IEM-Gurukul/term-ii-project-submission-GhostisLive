package com.chatapp.persistence;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class PersistenceManager {

    private final String logFilePath;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PersistenceManager(String logFilePath) {
        this.logFilePath = logFilePath;
        ensureFileExists();
    }

    
     // Saves a single message to the log file with a timestamp.
     
    public void saveMessage(String message) {
        String timestamped = "[" + LocalDateTime.now().format(FORMATTER) + "] " + message;
        writeToFile(timestamped);
    }

    /**
     * Appends a line to the log file.
     * Uses try-with-resources for safe file handling.
     */
    private void writeToFile(String line) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(logFilePath, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }

    /**
     * Creates the log file and parent directories if they don't exist.
     */
    private void ensureFileExists() {
        try {
            Path path = Paths.get(logFilePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            if (!Files.exists(path)) {
                Files.createFile(path);
                System.out.println("Log file created at: " + logFilePath);
            }
        } catch (IOException e) {
            System.err.println("Failed to create log file: " + e.getMessage());
        }
    }

    public String getLogFilePath() {
        return logFilePath;
    }
}