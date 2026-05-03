package com.example;

import javax.swing.*;
import java.awt.*;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientApp extends JFrame {
    private final String host;
    private final JTextField[] ratingFields = new JTextField[Constants.NAMES.length];
    private final JButton submitButton;

    public ClientApp(String host) {
        this.host = host;
        setTitle("Rating Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(Constants.NAMES.length + 1, 2, 10, 10));
        setSize(400, 250);

        for (int i = 0; i < Constants.NAMES.length; i++) {
            add(new JLabel("Rating for " + Constants.NAMES[i] + " (5-35):", SwingConstants.RIGHT));
            ratingFields[i] = new JTextField("20"); // Changed default to 20 so 5x20 = 100
            add(ratingFields[i]);
        }

        submitButton = new JButton("Submit Ratings");
        submitButton.addActionListener(e -> sendRatings());

        add(new JLabel("Total must be exactly 100", SwingConstants.RIGHT));
        add(submitButton);
    }

    private void sendRatings() {
        int[] ratings = new int[Constants.NAMES.length];
        int sum = 0;

        // 1. Validate, parse input, and check bounds/sum
        try {
            for (int i = 0; i < Constants.NAMES.length; i++) {
                int rating = Integer.parseInt(ratingFields[i].getText().trim());

                // Constraint: Minimum 5, Maximum 35
                if (rating < 5 || rating > 35) {
                    JOptionPane.showMessageDialog(this,
                            "Rating for " + Constants.NAMES[i] + " must be between 5 and 35.",
                            "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                    return; // Stop processing
                }

                ratings[i] = rating;
                sum += rating;
            }

            // Constraint: Total must exactly equal 100
            if (sum != 100) {
                JOptionPane.showMessageDialog(this,
                        "Total ratings must add up to exactly 100.\nYour current total is: " + sum,
                        "Total Error",
                        JOptionPane.ERROR_MESSAGE);
                return; // Stop processing
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers only.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Send data to server
        try (Socket socket = new Socket(host, Constants.PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(ratings);
            out.flush();

            JOptionPane.showMessageDialog(this, "Ratings submitted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            submitButton.setEnabled(false);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not connect to server. Is it running?", "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String askServerIp(Component parent) {
        String ip = JOptionPane.showInputDialog(
                parent,
                "Enter server IP address:",
                Constants.HOST
        );
        if (ip == null) {
            return null;
        }
        ip = ip.trim();
        return ip.isEmpty() ? null : ip;
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
            new ClientApp(host).setVisible(true);
        });
    }
}