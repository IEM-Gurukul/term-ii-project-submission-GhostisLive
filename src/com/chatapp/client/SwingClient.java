package com.chatapp.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class SwingClient extends JFrame {

    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;

    public SwingClient(String host, int port) {
        buildUI();
        connectToServer(host, port);
    }

    
    // Builds the Swing UI — chat area, input field, send button.
    
    private void buildUI() {
        setTitle("Chat Application");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        // Chat display area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        // Input panel at the bottom
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton  = new JButton("Send");

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // Send on button click or Enter key
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        // Handle window close — send /quit before closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (out != null) out.println("/quit");
                dispose();
                System.exit(0);
            }
        });

        setVisible(true);
    }

    
     // Connects to the ChatServer and starts the listener thread.
     
    private void connectToServer(String host, int port) {
        try {
            socket = new Socket(host, port);
            out    = new PrintWriter(socket.getOutputStream(), true);
            in     = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

  
            Thread listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (IOException e) {
            appendToChat("Could not connect to server: " + e.getMessage());
        }
    }

    /**
     * Listens for messages from server on a background thread.
     * Uses SwingUtilities.invokeLater() to update UI safely.
     */
    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                final String msg = message;
                SwingUtilities.invokeLater(() -> appendToChat(msg));
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() ->
                    appendToChat("Disconnected from server."));
        }
    }

    
      //Sends the typed message to the server.
     
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty() && out != null) {
            out.println(message);
            inputField.setText("");
        }
    }

   
     //Appends a message to the chat area safely.
     
    private void appendToChat(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        String host = (args.length > 0) ? args[0] : "localhost";
        int port    = (args.length > 1) ? Integer.parseInt(args[1]) : 5000;

        SwingUtilities.invokeLater(() -> new SwingClient(host, port));
    }
}