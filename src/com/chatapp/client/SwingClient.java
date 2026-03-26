package com.chatapp.client;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;


public class SwingClient extends JFrame {

    // Palette──────────────────────────────────────────────────────────────
    private static final Color BG_DEEP    = new Color(32,  36,  42);   
    private static final Color BG_SURFACE = new Color(40,  44,  52);
    private static final Color BG_CARD    = new Color(48,  52,  60);
    private static final Color BG_INPUT   = new Color(55,  60,  70);
    private static final Color ACCENT     = new Color(88, 166, 255);
    private static final Color ACCENT2    = new Color(56, 139, 253);
    private static final Color TEXT_PRI   = new Color(230, 237, 243);
    private static final Color TEXT_SEC   = new Color(125, 133, 144);
    private static final Color TEXT_MUT   = new Color(90,  96, 105);
    private static final Color ONLINE     = new Color(35, 197, 94);
    private static final Color MSG_SELF   = new Color(79, 140, 255);   
    private static final Color MSG_OTHER  = new Color(55,  60,  68);
    private static final Color BORDER_C   = new Color(48,  54,  61);
    private static final Color DM_COLOR   = new Color(245, 158, 11);
    private static final Color SYS_COLOR  = new Color(88, 166, 255);
    private static final Color TIME_COLOR = new Color(100, 108, 120);

    // Component 
    private JPanel  chatPanel;
    private JScrollPane scrollPane;
    private JTextField  inputField;
    private JButton     sendButton;
    private JLabel      statusLabel;
    private JLabel      userLabel;
    private JPanel      userListPanel;
    private JLabel      typingLabel;

    // Network 
    private PrintWriter  out;
    private BufferedReader in;
    private Socket       socket;
    private String       myUsername = "";

    private boolean isTyping = false;
    private javax.swing.Timer typingTimer;
    private java.util.Set<String> typingUsers = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public SwingClient(String host, int port) {
        buildUI();
        connectToServer(host, port);
    }

    // UI CONSTRUCTION
    private void buildUI() {
        setTitle("ChatApp");
        setSize(860, 620);
        setMinimumSize(new Dimension(680, 480));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setBackground(BG_DEEP);

        getRootPane().setBorder(BorderFactory.createLineBorder(BORDER_C, 1));

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_DEEP);
        setContentPane(root);

        root.add(buildSidebar(),    BorderLayout.WEST);
        root.add(buildChatArea(),   BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (out != null) out.println("/quit");
                dispose(); System.exit(0);
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 0));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBackground(BG_SURFACE);
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER_C));
        // Logo area
        JPanel logo = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
        logo.setBackground(BG_SURFACE);
        logo.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_C));

        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Dialog", Font.PLAIN, 10));
        dot.setForeground(ACCENT);

        JLabel title = new JLabel("ChatApp");
        title.setFont(new Font("Dialog", Font.BOLD, 14));
        title.setForeground(TEXT_PRI);

        logo.add(dot);
        logo.add(title);

        JPanel secHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        secHeader.setBackground(BG_SURFACE);
        JLabel secLabel = new JLabel("ONLINE");
        secLabel.setFont(new Font("Dialog", Font.BOLD, 10));
        secLabel.setForeground(TEXT_MUT);
        secHeader.add(secLabel);

        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBackground(BG_SURFACE);

        JScrollPane userScroll = new JScrollPane(userListPanel);
        userScroll.setBorder(BorderFactory.createEmptyBorder());
        userScroll.setBackground(BG_SURFACE);
        userScroll.getViewport().setBackground(BG_SURFACE);
        userScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG_SURFACE);
        top.add(logo,      BorderLayout.NORTH);
        top.add(secHeader, BorderLayout.CENTER);

        sidebar.add(top,        BorderLayout.NORTH);
        sidebar.add(userScroll, BorderLayout.CENTER);

        JPanel myInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        myInfo.setBackground(new Color(17, 21, 28));
        myInfo.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_C));

        userLabel = new JLabel("Connecting...");
        userLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        userLabel.setForeground(TEXT_PRI);

        JLabel onlineDot = new JLabel("●");
        onlineDot.setFont(new Font("Dialog", Font.PLAIN, 9));
        onlineDot.setForeground(ONLINE);

        myInfo.add(onlineDot);
        myInfo.add(userLabel);
        sidebar.add(myInfo, BorderLayout.SOUTH);

        return sidebar;
    }

 
    private JPanel buildChatArea() {
        JPanel area = new JPanel(new BorderLayout(0, 0));
        area.setBackground(BG_DEEP);

        area.add(buildHeader(),    BorderLayout.NORTH);
        area.add(buildMessages(),  BorderLayout.CENTER);
        area.add(buildInputBar(),  BorderLayout.SOUTH);

        return area;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setBackground(BG_SURFACE);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_C),
                new EmptyBorder(12, 18, 12, 18)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);

        JLabel hashLabel = new JLabel("# ");
        hashLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        hashLabel.setForeground(TEXT_SEC);

        JLabel roomLabel = new JLabel("general");
        roomLabel.setFont(new Font("Dialog", Font.BOLD, 15));
        roomLabel.setForeground(TEXT_PRI);

        left.add(hashLabel);
        left.add(roomLabel);

        statusLabel = new JLabel("● connecting...");
        statusLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_MUT);

        header.add(left,        BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);

        return header;
    }

    private JScrollPane buildMessages() {
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(BG_DEEP);
        chatPanel.setBorder(new EmptyBorder(16, 0, 8, 0));

        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(BG_DEEP);
        scrollPane.getViewport().setBackground(BG_DEEP);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor     = new Color(60, 68, 78);
                trackColor     = BG_DEEP;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b;
            }
        });

        return scrollPane;
    }

    private JPanel buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(BG_SURFACE);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER_C),
                new EmptyBorder(8, 16, 12, 16)));


        typingLabel = new JLabel(" ");
        typingLabel.setFont(new Font("Dialog", Font.ITALIC, 11));
        typingLabel.setForeground(TEXT_SEC);
        typingLabel.setBorder(new EmptyBorder(0, 4, 4, 0));

        JPanel inputWrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(BORDER_C);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
            }
        };
        inputWrapper.setOpaque(false);
        inputWrapper.setBorder(new EmptyBorder(8, 14, 8, 14));

        inputField = new JTextField();
        inputField.setOpaque(false);
        inputField.setBorder(BorderFactory.createEmptyBorder());
        inputField.setFont(new Font("Dialog", Font.PLAIN, 13));
        inputField.setForeground(TEXT_PRI);
        inputField.setCaretColor(ACCENT);
        inputField.putClientProperty("caretWidth", 2);

        // Placeholder text
        inputField.setText("Message #general  ·  /msg <user> <text> for DM  ·  /quit to exit");
        inputField.setForeground(TEXT_MUT);
        inputField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (inputField.getForeground().equals(TEXT_MUT)) {
                    inputField.setText("");
                    inputField.setForeground(TEXT_PRI);
                }
            }
        });

        inputField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onTyping(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onTyping(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onTyping(); }
        });

        typingTimer = new javax.swing.Timer(2000, e -> {
            if (isTyping && out != null) {
                out.println("/stoptyping");
                isTyping = false;
            }
        });
        typingTimer.setRepeats(false);

        inputWrapper.add(inputField, BorderLayout.CENTER);


        sendButton = new JButton("Send") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed() ? ACCENT2 :
                           getModel().isRollover() ? new Color(100, 175, 255) : ACCENT;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth()  - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        sendButton.setFont(new Font("Dialog", Font.BOLD, 12));
        sendButton.setPreferredSize(new Dimension(70, 36));
        sendButton.setBorderPainted(false);
        sendButton.setContentAreaFilled(false);
        sendButton.setFocusPainted(false);
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.setOpaque(false);
        inputRow.add(inputWrapper, BorderLayout.CENTER);
        inputRow.add(sendButton,   BorderLayout.EAST);

        bar.add(typingLabel, BorderLayout.NORTH);
        bar.add(inputRow,    BorderLayout.CENTER);

        return bar;
    }

    private void onTyping() {
        if (inputField.getForeground().equals(TEXT_MUT)) return; 
        if (!isTyping && out != null && !myUsername.isEmpty()) {
            out.println("/typing");
            isTyping = true;
        }
        typingTimer.restart();
    }

    private void updateTypingLabel() {
        SwingUtilities.invokeLater(() -> {
            if (typingUsers.isEmpty()) {
                typingLabel.setText(" ");
            } else if (typingUsers.size() == 1) {
                typingLabel.setText(typingUsers.iterator().next() + " is typing...");
            } else {
                typingLabel.setText(typingUsers.size() + " people are typing...");
            }
        });
    }

    // MESSAGE RENDERING
    private void appendMessage(String raw) {
        SwingUtilities.invokeLater(() -> {
            JPanel row = buildMessageRow(raw);
            chatPanel.add(row);
            chatPanel.add(Box.createVerticalStrut(2));
            chatPanel.revalidate();
            chatPanel.repaint();

            SwingUtilities.invokeLater(() -> {
                JScrollBar sb = scrollPane.getVerticalScrollBar();
                sb.setValue(sb.getMaximum());
            });
        });
    }

    private JPanel buildMessageRow(String raw) {

        boolean isSys = raw.startsWith("[") && raw.contains("has joined") ||
                        raw.startsWith("[") && raw.contains("has left") ||
                        raw.startsWith("Enter your") ||
                        raw.startsWith("Starting");
        boolean isDM  = raw.startsWith("[DM from") || raw.startsWith("[DM to") || raw.startsWith("[Server]");
        // Check if message is from self (with or without timestamp prefix)
        boolean isMine = !myUsername.isEmpty() && 
                        (raw.startsWith(myUsername + ": ") || 
                         raw.contains("] " + myUsername + ": "));

        if (isSys) return buildSystemMessage(raw);
        if (isDM)  return buildDMMessage(raw);
        return buildChatMessage(raw, isMine);
    }

    private JPanel buildSystemMessage(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Dialog", Font.PLAIN, 11));
        lbl.setForeground(SYS_COLOR);
        row.add(lbl);
        return row;
    }

    private JPanel buildDMMessage(String text) {
        // Parse DM format: "[DM from/to user] [HH:mm]: message"
        boolean isOutgoing = text.startsWith("[DM to ");
        String user = "";
        String timestamp = "";
        String message = text;

        // Extract user
        int userStart = text.indexOf("[DM ") + 4;
        int userEnd = text.indexOf("]");
        if (userStart > 4 && userEnd > userStart) {
            String userPart = text.substring(userStart, userEnd);
            if (userPart.startsWith("from ")) user = userPart.substring(5);
            else if (userPart.startsWith("to ")) user = userPart.substring(3);
        }

        // Extract timestamp and message
        int tsStart = text.indexOf("] [") + 3;
        int tsEnd = text.indexOf("]:", tsStart);
        if (tsStart > 3 && tsEnd > tsStart) {
            timestamp = text.substring(tsStart, tsEnd);
            message = text.substring(tsEnd + 3).trim();
        } else {
            // Fallback: old format without timestamp
            int msgStart = text.indexOf("]: ");
            if (msgStart > 0) message = text.substring(msgStart + 3);
        }

        JPanel outer = new JPanel(new FlowLayout(isOutgoing ? FlowLayout.RIGHT : FlowLayout.LEFT, 16, 4));
        outer.setOpaque(false);
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JPanel bubble = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isOutgoing ? new Color(50, 45, 25) : new Color(45, 35, 15));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(100, 80, 30));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
            }
        };
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Header with direction, user, and timestamp
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dirLabel = new JLabel(isOutgoing ? "To" : "From");
        dirLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        dirLabel.setForeground(TEXT_SEC);
        header.add(dirLabel);

        JLabel userLabel = new JLabel(user);
        userLabel.setFont(new Font("Dialog", Font.BOLD, 11));
        userLabel.setForeground(DM_COLOR);
        header.add(userLabel);

        if (!timestamp.isEmpty()) {
            JLabel timeLabel = new JLabel(timestamp);
            timeLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
            timeLabel.setForeground(TIME_COLOR);
            header.add(timeLabel);
        }

        bubble.add(header);
        bubble.add(Box.createVerticalStrut(3));

        JTextArea msgText = new JTextArea(message);
        msgText.setFont(new Font("Dialog", Font.PLAIN, 12));
        msgText.setForeground(new Color(255, 220, 150));
        msgText.setOpaque(false);
        msgText.setEditable(false);
        msgText.setFocusable(false);
        msgText.setLineWrap(true);
        msgText.setWrapStyleWord(true);
        msgText.setAlignmentX(Component.LEFT_ALIGNMENT);
        msgText.setBorder(BorderFactory.createEmptyBorder());
        msgText.setMaximumSize(new Dimension(350, Integer.MAX_VALUE));
        bubble.add(msgText);

        outer.add(bubble);
        return outer;
    }

    private JPanel buildChatMessage(String text, boolean isMine) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(2, 16, 2, 16));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        // Parse timestamp if present: "[HH:mm] username: message"
        String timestamp = "";
        String remaining = text;
        if (text.startsWith("[") && text.length() > 7 && text.charAt(6) == ']') {
            timestamp = text.substring(1, 6);  // "HH:mm"
            remaining = text.substring(8);     // skip "] "
        }

        // Parse "username: message"
        String sender  = "";
        String content = remaining;
        int colon = remaining.indexOf(": ");
        if (colon > 0) {
            sender  = remaining.substring(0, colon);
            content = remaining.substring(colon + 2);
        }

        // Avatar
        JPanel avatar = buildAvatar(sender.isEmpty() ? "?" : String.valueOf(sender.charAt(0)));

        final String ts = timestamp;

        // Message bubble
        JPanel bubble = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isMine ? MSG_SELF : MSG_OTHER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        bubble.setOpaque(false);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

        if (!sender.isEmpty()) {
            JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            headerRow.setOpaque(false);
            headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel senderLbl = new JLabel(sender);
            senderLbl.setFont(new Font("Dialog", Font.BOLD, 11));
            senderLbl.setForeground(isMine ? new Color(180, 210, 255) : ACCENT);
            headerRow.add(senderLbl);

            if (!ts.isEmpty()) {
                JLabel timeLbl = new JLabel(ts);
                timeLbl.setFont(new Font("Dialog", Font.PLAIN, 10));
                timeLbl.setForeground(TIME_COLOR);
                headerRow.add(timeLbl);
            }

            bubble.add(headerRow);
            bubble.add(Box.createVerticalStrut(2));
        }

        JTextArea msgText = new JTextArea(content);
        msgText.setFont(new Font("Dialog", Font.PLAIN, 13));
        msgText.setForeground(TEXT_PRI);
        msgText.setOpaque(false);
        msgText.setEditable(false);
        msgText.setFocusable(false);
        msgText.setLineWrap(true);
        msgText.setWrapStyleWord(true);
        msgText.setAlignmentX(Component.LEFT_ALIGNMENT);
        msgText.setBorder(BorderFactory.createEmptyBorder());
        msgText.setMaximumSize(new Dimension(480, Integer.MAX_VALUE));
        bubble.add(msgText);

        JPanel bubbleWrapper = new JPanel(new FlowLayout(
                isMine ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        bubbleWrapper.setOpaque(false);
        bubbleWrapper.add(bubble);

        if (!isMine) {
            row.add(avatar,        BorderLayout.WEST);
            row.add(bubbleWrapper, BorderLayout.CENTER);
        } else {
            row.add(bubbleWrapper, BorderLayout.CENTER);
        }

        return row;
    }

    private JPanel buildAvatar(String initial) {
        JPanel av = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Generate color from initial
                int hash = initial.hashCode();
                Color[] palette = {
                    new Color(88,166,255), new Color(63,185,80),
                    new Color(210,153,34), new Color(248,81,73),
                    new Color(163,113,247)
                };
                g2.setColor(palette[Math.abs(hash) % palette.length]);
                g2.fillOval(0, 0, 32, 32);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Dialog", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                String s = initial.toUpperCase();
                g2.drawString(s, (32 - fm.stringWidth(s))/2, (32 + fm.getAscent() - fm.getDescent())/2);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(32, 32); }
            @Override public Dimension getMinimumSize()   { return new Dimension(32, 32); }
        };
        av.setOpaque(false);
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(44, 40));
        wrapper.add(av);
        return wrapper;
    }

    private void addUserToSidebar(String username) {
        SwingUtilities.invokeLater(() -> {
            JPanel entry = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
            entry.setOpaque(false);
            entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

            JLabel dot = new JLabel("●");
            dot.setFont(new Font("Dialog", Font.PLAIN, 8));
            dot.setForeground(ONLINE);

            JLabel name = new JLabel(username);
            name.setFont(new Font("Dialog", Font.PLAIN, 12));
            name.setForeground(TEXT_PRI);

            entry.add(dot);
            entry.add(name);
            entry.setName(username);

            // Click to start DM with this user
            entry.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            entry.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    startDMWith(username);
                }
                @Override public void mouseEntered(MouseEvent e) {
                    entry.setBackground(BG_CARD);
                    entry.setOpaque(true);
                }
                @Override public void mouseExited(MouseEvent e) {
                    entry.setOpaque(false);
                }
            });

            userListPanel.add(entry);
            userListPanel.revalidate();
        });
    }

    private void startDMWith(String username) {
        if (username.equals(myUsername)) return;
        inputField.setText("/msg " + username + " ");
        inputField.setForeground(TEXT_PRI);
        inputField.requestFocus();
        inputField.setCaretPosition(inputField.getText().length());
    }

    private void removeUserFromSidebar(String username) {
        SwingUtilities.invokeLater(() -> {
            for (Component c : userListPanel.getComponents()) {
                if (username.equals(c.getName())) {
                    userListPanel.remove(c);
                    break;
                }
            }
            userListPanel.revalidate();
            userListPanel.repaint();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NETWORK
    // ─────────────────────────────────────────────────────────────────────────

    private void connectToServer(String host, int port) {
        try {
            socket = new Socket(host, port);
            out    = new PrintWriter(socket.getOutputStream(), true);
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            setStatus("● connected to " + host + ":" + port, ONLINE);

            Thread listener = new Thread(this::listenForMessages);
            listener.setDaemon(true);
            listener.start();

        } catch (IOException e) {
            setStatus("● connection failed", new Color(248, 81, 73));
            appendMessage("Could not connect to server: " + e.getMessage());
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                final String msg = message;

                // Capture username from server prompt response
                if (msg.startsWith("Enter your username")) {
                    appendMessage(msg);
                    continue;
                }

                // Handle online users list from server
                if (msg.startsWith("[ONLINE_USERS]")) {
                    String userList = msg.substring(14); // skip "[ONLINE_USERS]"
                    if (!userList.isEmpty()) {
                        for (String user : userList.split(",")) {
                            if (!user.trim().isEmpty() && !user.trim().equals(myUsername)) {
                                addUserToSidebar(user.trim());
                            }
                        }
                    }
                    continue;
                }

                // Handle typing indicator signals
                if (msg.startsWith("[TYPING]")) {
                    String[] parts = msg.substring(8).split(":");
                    if (parts.length == 2) {
                        String user = parts[0];
                        boolean typing = parts[1].equals("1");
                        if (typing) {
                            typingUsers.add(user);
                        } else {
                            typingUsers.remove(user);
                        }
                        updateTypingLabel();
                    }
                    continue;
                }

                // Detect join/leave for sidebar updates
                if (msg.contains("has joined the chat")) {
                    // Format: "[username has joined the chat]"
                    int start = msg.indexOf("[");
                    int end = msg.indexOf(" has joined");
                    if (start >= 0 && end > start) {
                        String user = msg.substring(start + 1, end).trim();
                        if (!user.equals(myUsername)) addUserToSidebar(user);
                    }
                }
                if (msg.contains("has left the chat")) {
                    // Format: "[username has left the chat]"
                    int start = msg.indexOf("[");
                    int end = msg.indexOf(" has left");
                    if (start >= 0 && end > start) {
                        String user = msg.substring(start + 1, end).trim();
                        removeUserFromSidebar(user);
                    }
                }

                appendMessage(msg);
            }
        } catch (IOException e) {
            setStatus("● disconnected", new Color(248, 81, 73));
            appendMessage("[Disconnected from server]");
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || inputField.getForeground().equals(TEXT_MUT)) return;

        // Stop typing indicator when message is sent
        if (isTyping && out != null) {
            out.println("/stoptyping");
            isTyping = false;
            typingTimer.stop();
        }

        // Capture username on first message
        if (myUsername.isEmpty() && !text.startsWith("/")) {
            myUsername = text;
            userLabel.setText(myUsername);
            addUserToSidebar(myUsername);
        }

        if (out != null) out.println(text);
        inputField.setText("");
    }

    private void setStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            statusLabel.setForeground(color);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        // Use system look for native decorations, override colors ourselves
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}

        String host = (args.length > 0) ? args[0] : "localhost";
        int port    = (args.length > 1) ? Integer.parseInt(args[1]) : 5000;

        SwingUtilities.invokeLater(() -> new SwingClient(host, port));
    }
}