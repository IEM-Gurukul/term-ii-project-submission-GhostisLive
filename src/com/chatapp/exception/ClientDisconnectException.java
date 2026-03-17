package com.chatapp.exception;

public class ClientDisconnectException extends Exception {

    private final String username;

    public ClientDisconnectException(String username) {
        super("Client disconnected:" + username);
        this.username = username;
    }

    public ClientDisconnectException(String username, Throwable cause) {
        super("Client disconnected:" + username, cause);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
    
}
