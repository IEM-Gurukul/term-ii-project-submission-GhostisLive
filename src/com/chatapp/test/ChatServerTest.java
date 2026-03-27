package com.chatapp.test;

import com.chatapp.server.ChatServer;

public class ChatServerTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {

        System.out.println("  ChatServer Unit Tests");

        testRegisterObserver();
        testBroadcastReachesAllObservers();
        testUnregisterObserver();
        testBroadcastAfterUnregister();
        testMultipleMessages();
        testEmptyBroadcast();

        System.out.println("  Results: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    //Test 1 
    private static void testRegisterObserver() {
        ChatServer server = new ChatServer(0);
        MockObserver alice = new MockObserver("Alice");

        server.register(alice);
        server.broadcast("Hello");

        assert_true("register: observer receives broadcast after registering",
            alice.received("Hello"));
    }
    //Test 2 
    private static void testBroadcastReachesAllObservers() {
        ChatServer server = new ChatServer(0);
        MockObserver alice = new MockObserver("Alice");
        MockObserver bob   = new MockObserver("Bob");

        server.register(alice);
        server.register(bob);
        server.broadcast("Hi everyone");

        assert_true("broadcast: Alice receives message",
            alice.received("Hi everyone"));
        assert_true("broadcast: Bob receives message",
            bob.received("Hi everyone"));
    }

    //Test 3 
    private static void testUnregisterObserver() {
        ChatServer server = new ChatServer(0);
        MockObserver alice = new MockObserver("Alice");

        server.register(alice);
        server.unregister(alice);
        server.broadcast("Are you there?");

        assert_true("unregister: observer does NOT receive message after unregistering",
            !alice.received("Are you there?"));
    }

    //Test 4 
    private static void testBroadcastAfterUnregister() {
        ChatServer server = new ChatServer(0);
        MockObserver alice = new MockObserver("Alice");
        MockObserver bob   = new MockObserver("Bob");

        server.register(alice);
        server.register(bob);
        server.unregister(alice);
        server.broadcast("Bob only");

        assert_true("unregister: Bob still receives message",
            bob.received("Bob only"));
        assert_true("unregister: Alice does NOT receive message",
            !alice.received("Bob only"));
    }

    //Test 5
    private static void testMultipleMessages() {
        ChatServer server = new ChatServer(0);
        MockObserver alice = new MockObserver("Alice");

        server.register(alice);
        server.broadcast("Message 1");
        server.broadcast("Message 2");
        server.broadcast("Message 3");

        assert_equals("multiple messages: observer receives all 3",
            true, alice.received("Message 1") &&
                  alice.received("Message 2") &&
                  alice.received("Message 3"));
    }

    //Test 6 
    private static void testEmptyBroadcast() {
        ChatServer server = new ChatServer(0);
        MockObserver alice = new MockObserver("Alice");

        server.register(alice);
        server.broadcast("");
        assert_true("empty broadcast: observer still receives it",
            alice.getMessageCount() >= 1);
    }

    //Assertion helpers
    private static void assert_true(String testName, boolean condition) {
        if (condition) {
            System.out.println("  ✔ PASS  " + testName);
            passed++;
        } else {
            System.out.println("  ✘ FAIL  " + testName);
            failed++;
        }
    }

    private static void assert_equals(String testName, Object expected, Object actual) {
        if (expected.equals(actual)) {
            System.out.println("  ✔ PASS  " + testName);
            passed++;
        } else {
            System.out.println("  ✘ FAIL  " + testName + " [expected=" + expected + ", got=" + actual + "]");
            failed++;
        }
    }
}