package com.chatapp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * ConsoleClient - text-based client for the chat server.
 *
 * OOP Concept - Threads: runs a separate listener thread to
 * receive incoming messages without blocking the main thread
 * that handles user input.
 *
 * OOP Concept - Encapsulation: socket, reader and writer are
 * private; connection logic is contained within connect().
 */
public class ConsoleClient {

    private final String host;
    private final int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public ConsoleClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Connects to the server and starts the listener thread.
     */
    public void connect() {
        try {
            socket = new Socket(host, port);
            out    = new PrintWriter(socket.getOutputStream(), true);
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Connected to server at " + host + ":" + port);

            // Listener thread — receives messages from server
            // without blocking the input loop below
            Thread listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();

            // Main thread — reads user input and sends to server
            sendMessages();

        } catch (IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
        } finally {
            close();
        }
    }

    /**
     * Listens for incoming messages from the server.
     * Runs on its own daemon thread.
     */
    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server.");
        }
    }

    /**
     * Reads user input from console and sends to server.
     * Type /quit to exit.
     */
    private void sendMessages() throws IOException {
        BufferedReader console = new BufferedReader(
                new InputStreamReader(System.in));
        String input;
        while ((input = console.readLine()) != null) {
            out.println(input);
            if (input.equalsIgnoreCase("/quit")) {
                break;
            }
        }
    }

    /**
     * OOP Concept - Exception Handling: closes all resources
     * safely regardless of how the session ended.
     */
    private void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String host = (args.length > 0) ? args[0] : "localhost";
        int port    = (args.length > 1) ? Integer.parseInt(args[1]) : 5000;

        ConsoleClient client = new ConsoleClient(host, port);
        client.connect();
    }
}