package com.chatapp.test;

import com.chatapp.server.ChatServer;

import java.io.*;
import java.net.Socket;

public class IntegrationTest {

    private static final int TEST_PORT = 5050;
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("  Integration Tests");

        ChatServer server = new ChatServer(TEST_PORT);
        Thread serverThread = new Thread(server::start);
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(300);

        testClientConnectsAndReceivesBroadcast();
        testPrivateMessageReachesOnlyTarget();
        testClientDisconnectNotifiesOthers();

        System.out.println("  Results: " + passed + " passed, " + failed + " failed");
        server.stop();
        if (failed > 0) System.exit(1);
    }

    //Test 1 
    private static void testClientConnectsAndReceivesBroadcast() throws Exception {
        Socket alice = new Socket("localhost", TEST_PORT);
        Socket bob   = new Socket("localhost", TEST_PORT);

        PrintWriter aliceOut = new PrintWriter(alice.getOutputStream(), true);
        PrintWriter bobOut   = new PrintWriter(bob.getOutputStream(),   true);

        BufferedReader aliceIn = new BufferedReader(new InputStreamReader(alice.getInputStream()));
        BufferedReader bobIn   = new BufferedReader(new InputStreamReader(bob.getInputStream()));

        aliceIn.readLine(); 
        aliceOut.println("Alice");
        Thread.sleep(100);

        bobIn.readLine();    
        bobOut.println("Bob");
        Thread.sleep(200);

        aliceOut.println("Hello from Alice");
        Thread.sleep(200);

        // Bob should receive it
        String received = "";
        while (bobIn.ready()) {
            String line = bobIn.readLine();
            if (line != null && line.contains("Hello from Alice")) {
                received = line;
            }
        }

        assert_true("integration: Bob receives Alice's broadcast message",
            received.contains("Hello from Alice"));

        alice.close();
        bob.close();
        Thread.sleep(100);
    }

    //Test 2
    private static void testPrivateMessageReachesOnlyTarget() throws Exception {
        Socket alice   = new Socket("localhost", TEST_PORT);
        Socket bob     = new Socket("localhost", TEST_PORT);
        Socket charlie = new Socket("localhost", TEST_PORT);

        PrintWriter aliceOut   = new PrintWriter(alice.getOutputStream(),   true);
        PrintWriter bobOut     = new PrintWriter(bob.getOutputStream(),     true);
        PrintWriter charlieOut = new PrintWriter(charlie.getOutputStream(), true);

        BufferedReader aliceIn   = new BufferedReader(new InputStreamReader(alice.getInputStream()));
        BufferedReader bobIn     = new BufferedReader(new InputStreamReader(bob.getInputStream()));
        BufferedReader charlieIn = new BufferedReader(new InputStreamReader(charlie.getInputStream()));

        aliceIn.readLine();   aliceOut.println("Alice2");   Thread.sleep(100);
        bobIn.readLine();     bobOut.println("Bob2");       Thread.sleep(100);
        charlieIn.readLine(); charlieOut.println("Charlie"); Thread.sleep(200);

        // Drain join messages
        while (aliceIn.ready())   aliceIn.readLine();
        while (bobIn.ready())     bobIn.readLine();
        while (charlieIn.ready()) charlieIn.readLine();
        Thread.sleep(100);

        // Alice sends DM to Bob only
        aliceOut.println("/msg Bob2 Secret message");
        Thread.sleep(200);

        boolean bobGotDM     = false;
        boolean charlieGotDM = false;

        while (bobIn.ready()) {
            String line = bobIn.readLine();
            if (line != null && line.contains("Secret message")) bobGotDM = true;
        }
        while (charlieIn.ready()) {
            String line = charlieIn.readLine();
            if (line != null && line.contains("Secret message")) charlieGotDM = true;
        }

        assert_true("private message: Bob receives the DM",     bobGotDM);
        assert_true("private message: Charlie does NOT get DM", !charlieGotDM);

        alice.close(); bob.close(); charlie.close();
        Thread.sleep(100);
    }

    //Test 3 
    private static void testClientDisconnectNotifiesOthers() throws Exception {
        Socket alice = new Socket("localhost", TEST_PORT);
        Socket bob   = new Socket("localhost", TEST_PORT);

        PrintWriter aliceOut = new PrintWriter(alice.getOutputStream(), true);
        PrintWriter bobOut   = new PrintWriter(bob.getOutputStream(),   true);

        BufferedReader aliceIn = new BufferedReader(new InputStreamReader(alice.getInputStream()));
        BufferedReader bobIn   = new BufferedReader(new InputStreamReader(bob.getInputStream()));

        aliceIn.readLine(); aliceOut.println("AliceLeave"); Thread.sleep(100);
        bobIn.readLine();   bobOut.println("BobStay");      Thread.sleep(200);

        while (aliceIn.ready()) aliceIn.readLine();
        while (bobIn.ready())   bobIn.readLine();
        Thread.sleep(100);

        aliceOut.println("/quit");
        Thread.sleep(300);


        boolean sawLeave = false;
        while (bobIn.ready()) {
            String line = bobIn.readLine();
            if (line != null && line.contains("has left the chat")) sawLeave = true;
        }

        assert_true("disconnect: Bob sees Alice's leave notification", sawLeave);

        bob.close();
        Thread.sleep(100);
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
}