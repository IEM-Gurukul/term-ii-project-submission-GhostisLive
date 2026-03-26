package com.chatapp.client;

import com.chatapp.exception.ClientDisconnectException;
import com.chatapp.observer.ChatObserver;
import com.chatapp.server.ChatServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


public class ClientHandler extends Thread implements ChatObserver {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final Socket socket;
    private final ChatServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("Enter your username: ");
            username = in.readLine();

            if (username == null || username.isBlank()) {
                throw new ClientDisconnectException("unknown");
            }

            java.util.List<String> onlineUsers = server.getOnlineUsers();
            if (!onlineUsers.isEmpty()) {
                out.println("[ONLINE_USERS]" + String.join(",", onlineUsers));
            }

            server.register(this);
            server.broadcast("[" + username + " has joined the chat]");

            listenForMessages();

        } catch (ClientDisconnectException e) {
            
            System.err.println("Client left before registering: " + e.getMessage());
        } catch (SocketException e) {
            
            System.err.println("Socket error for " + username + ": " + e.getMessage());
        } catch (IOException e) {
            
            System.err.println("I/O error for " + username + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void listenForMessages() throws IOException {
        String message;
        while ((message = in.readLine()) != null) {
            if (message.equalsIgnoreCase("/quit")) {
                System.out.println(username + " has quit.");
                break;
            }

            if (message.equals("/typing")) {
                server.broadcastTyping(username, true);
                continue;
            }
            if (message.equals("/stoptyping")) {
                server.broadcastTyping(username, false);
                continue;
            }
            
            if (message.startsWith("/msg ")) {
                handlePrivateMessage(message);
            } else {
                String timestamp = LocalTime.now().format(TIME_FMT);
                server.broadcast("[" + timestamp + "] " + username + ": " + message);
            }
        }
    }

    private void handlePrivateMessage(String input) {
        String[] parts = input.split(" ", 3);
        if (parts.length < 3) {
            out.println("[Server] Usage: /msg <username> <message>");
            return;
        }
        String targetUsername = parts[1];
        String privateMessage = parts[2];
        String timestamp = LocalTime.now().format(TIME_FMT);
        boolean sent = server.sendPrivateMessage(username, targetUsername, privateMessage, timestamp);
        if (sent) {
            // Send confirmation to sender
            out.println("[DM to " + targetUsername + "] [" + timestamp + "]: " + privateMessage);
        } else {
            out.println("[Server] User '" + targetUsername + "' not found.");
        }
    }

    @Override
    public void update(String message) {
        if (out != null) {
            out.println(message);
        }
    }

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
            System.err.println("Error closing socket for " + username + ": " + e.getMessage());
        }
    }

    public String getUsername() {
        return username;
    }
}