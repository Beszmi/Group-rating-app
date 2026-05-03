package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientApp extends JFrame {
    private final SessionConfig session;
    private final Socket socket;
    private final ObjectOutputStream out;
    private final JTextField[] ratingFields;
    private final JButton submitButton;

    public ClientApp(SessionConfig session, Socket socket, ObjectOutputStream out) {
        this.session = session;
        this.socket = socket;
        this.out = out;

        int n = session.getMemberCount();
        int min = session.getMinRating();
        int max = session.getMaxRating();

        setTitle("Rating Client (" + n + " members, " + min + "-" + max + "% each)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(n + 1, 2, 10, 10));
        setSize(440, Math.max(220, 36 * (n + 2)));

        ratingFields = new JTextField[n];
        int[] defaults = defaultRatings(n);

        for (int i = 0; i < n; i++) {
            String name = session.getNames()[i];
            add(new JLabel("Rating for " + name + " (" + min + "-" + max + "):", SwingConstants.RIGHT));
            ratingFields[i] = new JTextField(String.valueOf(defaults[i]), 6);
            add(ratingFields[i]);
        }

        submitButton = new JButton("Submit Ratings");
        submitButton.addActionListener(e -> sendRatings(min, max));

        add(new JLabel("Total must be exactly 100", SwingConstants.RIGHT));
        add(submitButton);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    socket.close();
                } catch (Exception ignored) {
                }
            }
        });
    }

    private static int[] defaultRatings(int n) {
        int[] v = new int[n];
        int base = 100 / n;
        int rem = 100 % n;
        for (int i = 0; i < n; i++) {
            v[i] = base + (i < rem ? 1 : 0);
        }
        return v;
    }

    private void sendRatings(int min, int max) {
        int n = session.getMemberCount();
        int[] ratings = new int[n];
        int sum = 0;
        String[] names = session.getNames();

        try {
            for (int i = 0; i < n; i++) {
                int rating = Integer.parseInt(ratingFields[i].getText().trim());

                if (rating < min || rating > max) {
                    JOptionPane.showMessageDialog(this,
                            "Rating for " + names[i] + " must be between " + min + " and " + max + ".",
                            "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                ratings[i] = rating;
                sum += rating;
            }

            if (sum != 100) {
                JOptionPane.showMessageDialog(this,
                        "Total ratings must add up to exactly 100.\nYour current total is: " + sum,
                        "Total Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers only.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            out.writeObject(ratings);
            out.flush();

            JOptionPane.showMessageDialog(this, "Ratings submitted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            submitButton.setEnabled(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not send ratings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static String askServerIp(Component parent) {
        String ip = JOptionPane.showInputDialog(
                parent,
                "Enter server IP address:",
                "127.0.0.1"
        );
        if (ip == null) {
            return null;
        }
        ip = ip.trim();
        return ip.isEmpty() ? null : ip;
    }

    private static Integer askServerPort(Component parent) {
        String p = JOptionPane.showInputDialog(
                parent,
                "Enter server port:",
                String.valueOf(Constants.DEFAULT_PORT)
        );
        if (p == null) {
            return null;
        }
        p = p.trim();
        try {
            int port = Integer.parseInt(p);
            if (port >= 1 && port <= 65535) {
                return port;
            }
        } catch (NumberFormatException ignored) {
        }
        JOptionPane.showMessageDialog(parent, "Port must be a number from 1 to 65535.", "Invalid port", JOptionPane.ERROR_MESSAGE);
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String host = askServerIp(null);
            if (host == null) {
                JOptionPane.showMessageDialog(
                        null,
                        "Server IP is required. Client start cancelled.",
                        "Setup Cancelled",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            Integer port = askServerPort(null);
            if (port == null) {
                return;
            }

            try {
                Socket socket = new Socket(host, port);
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                SessionConfig session = (SessionConfig) in.readObject();
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

                new ClientApp(session, socket, out).setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "Could not connect to server or read session. Is it running on " + host + ":" + port + "?",
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}
