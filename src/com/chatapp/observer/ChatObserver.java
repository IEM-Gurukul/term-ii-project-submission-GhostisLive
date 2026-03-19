package com.chatapp.observer;

/**
 * OOP Concept - Abstraction: The server depends only on
 * this interface, never on concrete implementations.
 */

public interface ChatObserver {
    public void update(String message);
    
}
