package com.chatapp.test;

import com.chatapp.observer.ChatObserver;
import java.util.ArrayList;
import java.util.List;

public class MockObserver implements ChatObserver {

    private final List<String> receivedMessages = new ArrayList<>();
    private final String name;

    public MockObserver(String name) {
        this.name = name;
    }
    @Override
    public void update(String message) {
        receivedMessages.add(message);
    }

    public List<String> getReceivedMessages() {
        return receivedMessages;
    }

    public String getLastMessage() {
        if (receivedMessages.isEmpty()) return null;
        return receivedMessages.get(receivedMessages.size() - 1);
    }

    public boolean received(String message) {
        return receivedMessages.contains(message);
    }

    public int getMessageCount() {
        return receivedMessages.size();
    }

    public void clear() {
        receivedMessages.clear();
    }

    public String getName() {
        return name;
    }
}