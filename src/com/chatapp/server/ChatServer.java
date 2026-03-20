package com.chatapp.server;

import com.chatapp.observer.ChatObserver;
import com.chatapp.client.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ChatServer {

    private final int port;
    private final List<ChatObserver> observers;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;

    private static final int MAX_THREADS = 50;

    public ChatServer(int port) {
        this.port = port;
        this.observers = new CopyOnWriteArrayList<>();
        this.threadPool = Executors.newFixedThreadPool(MAX_THREADS);
    }

    /** Register a new observer (client or logger). */
    public void register(ChatObserver observer) {
        observers.add(observer);
    }

    /** Remove an observer when a client disconnects.     */
    public void unregister(ChatObserver observer) {
        observers.remove(observer);
    }

    public void broadcast(String message) {
    System.out.println("[BROADCAST] " + message);
    for (ChatObserver observer : observers) {
        observer.update(message);
    }
}

    
      /** Start the server and accept incoming connections.*/
     
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
            acceptClients();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    /** Continuously accept new client connections. */
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

    /** Gracefully shut down the server and thread pool.   */
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