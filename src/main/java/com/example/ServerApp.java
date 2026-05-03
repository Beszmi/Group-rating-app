package com.example;

import javax.swing.*;
import java.awt.*;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp extends JFrame {
    private final String[] names;
    private final JLabel[] scoreLabels;
    private final int[] totalScores;
    private int submissionCount = 0;

    public ServerApp(String[] names) {
        this.names = names.clone();
        this.scoreLabels = new JLabel[this.names.length];
        this.totalScores = new int[this.names.length];

        setTitle("Rating Server (Host)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(this.names.length + 1, 1, 10, 10));
        setSize(300, 250);

        JLabel titleLabel = new JLabel("Átlagos százalékok:", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(titleLabel);

        for (int i = 0; i < this.names.length; i++) {
            scoreLabels[i] = new JLabel(this.names[i] + ": Waiting for data...", SwingConstants.CENTER);
            add(scoreLabels[i]);
        }

        startServer();
    }

    private void startServer() {
        // Start the server socket on a background thread
        Thread.startVirtualThread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(Constants.PORT)) {
                System.out.println("Server started on port " + Constants.PORT);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    // Handle each client connection in a new virtual thread
                    Thread.ofVirtual().start(() -> handleClient(clientSocket));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void handleClient(Socket socket) {
        try (socket; ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            // Read the array of 5 integers sent by the client
            int[] ratings = (int[]) in.readObject();

            if (ratings != null && ratings.length == names.length) {
                updateAverages(ratings);
            }
        } catch (Exception e) {
            System.err.println("Error reading from client: " + e.getMessage());
        }
    }

    private synchronized void updateAverages(int[] newRatings) {
        submissionCount++;
        for (int i = 0; i < totalScores.length; i++) {
            totalScores[i] += newRatings[i];
        }

        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < names.length; i++) {
                double average = (double) totalScores[i] / submissionCount;
                scoreLabels[i].setText(String.format("%s: %.2f (from %d clients)",
                        names[i], average, submissionCount));
            }
        });
    }

    private static String[] askNames(Component parent) {
        JPanel panel = new JPanel(new GridLayout(Constants.NAMES.length, 2, 8, 8));
        JTextField[] nameFields = new JTextField[Constants.NAMES.length];

        for (int i = 0; i < Constants.NAMES.length; i++) {
            panel.add(new JLabel("Person " + (i + 1) + " name:", SwingConstants.RIGHT));
            nameFields[i] = new JTextField(Constants.NAMES[i], 14);
            panel.add(nameFields[i]);
        }

        int result = JOptionPane.showConfirmDialog(
                parent,
                panel,
                "Set person names",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        String[] enteredNames = new String[Constants.NAMES.length];
        for (int i = 0; i < enteredNames.length; i++) {
            String value = nameFields[i].getText().trim();
            if (value.isEmpty()) {
                return null;
            }
            enteredNames[i] = value;
        }
        return enteredNames;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String[] names = askNames(null);
            if (names == null) {
                JOptionPane.showMessageDialog(
                        null,
                        "All names are required. Server start cancelled.",
                        "Setup Cancelled",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            new ServerApp(names).setVisible(true);
        });
    }
}