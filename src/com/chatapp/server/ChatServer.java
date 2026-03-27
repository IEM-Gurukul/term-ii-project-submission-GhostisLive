package com.chatapp.server;

import com.chatapp.client.ClientHandler;
import com.chatapp.observer.ChatObserver;
import com.chatapp.persistence.MessageLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatServer {

    private final int port;
    private final List<ChatObserver> observers;
    private final ExecutorService threadPool;
    private final LinkedBlockingQueue<String> messageQueue;
    private ServerSocket serverSocket;

    private static final int MAX_THREADS = 50;

    public ChatServer(int port) {
        this.port = port;
        this.observers = new CopyOnWriteArrayList<>();
        this.threadPool = Executors.newFixedThreadPool(MAX_THREADS);
        this.messageQueue = new LinkedBlockingQueue<>();
        MessageLogger logger = new MessageLogger("logs/chat_history.txt");
        this.observers.add(logger);
    }

    public void register(ChatObserver observer) {
        observers.add(observer);
    }

    public void unregister(ChatObserver observer) {
        observers.remove(observer);
    }

    /**
     * Queues a message for broadcast.
     * LinkedBlockingQueue ensures thread-safe access from
     * multiple ClientHandler threads simultaneously.
     */
    public void broadcast(String message) {
        try {
            messageQueue.put(message);
            processQueue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Broadcast interrupted: " + e.getMessage());
        }
    }

    /**
     * Drains the message queue and notifies all observers.
     * OOP Concept - Polymorphism: update() is called on every
     * ChatObserver reference — identical call regardless of type.
     */
    private void processQueue() {
        String message;
        while ((message = messageQueue.poll()) != null) {
            System.out.println("[BROADCAST] " + message);
            for (ChatObserver observer : observers) {
                observer.update(message);
            }
        }
    }

    /**
     * Sends a private message to a specific user by username.
     * Returns true if the target user was found, false otherwise.
     */
    public boolean sendPrivateMessage(String from, String toUsername, String message, String timestamp) {
        for (ChatObserver observer : observers) {
            if (observer instanceof ClientHandler handler) {
                if (handler.getUsername().equalsIgnoreCase(toUsername)) {
                    handler.update("[DM from " + from + "] [" + timestamp + "]: " + message);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a list of all currently online usernames.
     */
    public java.util.List<String> getOnlineUsers() {
        java.util.List<String> users = new java.util.ArrayList<>();
        for (ChatObserver observer : observers) {
            if (observer instanceof ClientHandler handler) {
                String name = handler.getUsername();
                if (name != null && !name.isBlank()) {
                    users.add(name);
                }
            }
        }
        return users;
    }

    /**
     * Broadcasts typing indicator to all clients except the typer.
     */
    public void broadcastTyping(String username, boolean isTyping) {
        String signal = "[TYPING]" + username + ":" + (isTyping ? "1" : "0");
        for (ChatObserver observer : observers) {
            if (observer instanceof ClientHandler handler) {
                if (!handler.getUsername().equals(username)) {
                    handler.update(signal);
                }
            }
        }
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            stop();
        }));
            acceptClients();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    private void acceptClients() {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                threadPool.execute(handler);
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        try {
            if (serverSocket != null) serverSocket.close();
            threadPool.shutdown();
            System.out.println("Server stopped.");
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }
}