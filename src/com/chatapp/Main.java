package com.chatapp;

import com.chatapp.server.ChatServer;

/**
 * Application entry point.
 * Starts the ChatServer on the specified port.
 */
public class Main {

    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        System.out.println("Starting Chat Server on port " + port + "...");

        ChatServer server = new ChatServer(port);
        server.start();
    }
}