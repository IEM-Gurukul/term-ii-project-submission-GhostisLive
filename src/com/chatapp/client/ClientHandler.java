package com.chatapp.client;

import com.chatapp.exception.ClientDisconnectException;
import com.chatapp.observer.ChatObserver;
import com.chatapp.server.ChatServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread implements ChatObserver {

    private final Socket socket;
    private final ChatServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }
    /**
     * OOP Concept - Threads: run() executes on its own thread.
     * Reads messages from client and broadcasts to all observers.
     */
    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // First message from client is their username
            out.println("Enter your username: ");
            username = in.readLine();

            if (username == null || username.isBlank()) {
                throw new ClientDisconnectException("unknown");
            }

            server.register(this);
            server.broadcast("[" + username + " has joined the chat]");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("/quit")) {
                    break;
                }
                server.broadcast(username + ": " + message);
            }

        } catch (IOException e) {
            System.err.println("Connection error for " + username + ": " + e.getMessage());
        } catch (ClientDisconnectException e) {
            System.err.println(e.getMessage());
        } finally {
            disconnect();
        }
    }

    /**
     * OOP Concept - Polymorphism: update() is called by
     * ChatServer on every ChatObserver — this implementation
     * sends the message out to this client's socket.
     */
    @Override
    public void update(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /**
     * OOP Concept - Exception Handling: handles disconnect
     * cleanly — unregisters observer and closes socket.
     */
    private void disconnect() {
        try {
            server.unregister(this);
            if (username != null) {
                server.broadcast("[" + username + " has left the chat]");
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }

    public String getUsername() {
        return username;
    }
}
