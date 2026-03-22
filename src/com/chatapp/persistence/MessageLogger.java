package com.chatapp.persistence;

import com.chatapp.observer.ChatObserver;


public class MessageLogger implements ChatObserver {

    private final PersistenceManager persistenceManager;

    public MessageLogger(String logFilePath) {
        this.persistenceManager = new PersistenceManager(logFilePath);
        System.out.println("MessageLogger active. Logging to: " + logFilePath);
    }

    /**
     * Called by ChatServer on every broadcast.
     * Delegates storage to PersistenceManager.
     */
    @Override
    public void update(String message) {
        persistenceManager.saveMessage(message);
    }
}