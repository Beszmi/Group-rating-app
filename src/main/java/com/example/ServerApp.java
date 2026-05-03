package com.example;

import javax.swing.*;
import java.awt.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp extends JFrame {
    private final String[] names;
    private final int port;
    private final JLabel[] scoreLabels;
    private final int[] totalScores;
    private int submissionCount = 0;

    public ServerApp(int port, String[] names) {
        this.port = port;
        this.names = names.clone();
        this.scoreLabels = new JLabel[this.names.length];
        this.totalScores = new int[this.names.length];

        setTitle("Rating Server (Host) — port " + port);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(this.names.length + 1, 1, 10, 10));
        setSize(360, Math.max(200, 40 * (this.names.length + 2)));

        JLabel titleLabel = new JLabel("Average %:", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(titleLabel);

        for (int i = 0; i < this.names.length; i++) {
            scoreLabels[i] = new JLabel(this.names[i] + ": Waiting for data...", SwingConstants.CENTER);
            add(scoreLabels[i]);
        }

        startServer();
    }

    private void startServer() {
        Thread.startVirtualThread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Server started on port " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    Thread.ofVirtual().start(() -> handleClient(clientSocket));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void handleClient(Socket socket) {
        try (socket) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            int[] bounds = RatingBounds.compute(names.length);
            out.writeObject(new SessionConfig(names, bounds[0], bounds[1]));
            out.flush();

            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            int[] ratings = (int[]) in.readObject();

            if (ratings != null && ratings.length == names.length) {
                updateAverages(ratings);
            }
        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
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

    private static Integer parsePort(String text) {
        try {
            int p = Integer.parseInt(text.trim());
            if (p >= 1 && p <= 65535) {
                return p;
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static ServerSetup askServerSetup(Component parent) {
        JPanel root = new JPanel(new BorderLayout(8, 8));

        JPanel top = new JPanel(new GridLayout(2, 2, 8, 8));
        top.add(new JLabel("Port:", SwingConstants.RIGHT));
        JTextField portField = new JTextField(String.valueOf(Constants.DEFAULT_PORT), 8);
        top.add(portField);
        top.add(new JLabel("Number of members:", SwingConstants.RIGHT));
        SpinnerNumberModel countModel = new SpinnerNumberModel(5, RatingBounds.MIN_MEMBERS, RatingBounds.MAX_MEMBERS, 1);
        JSpinner countSpinner = new JSpinner(countModel);
        top.add(countSpinner);

        JPanel namesPanel = new JPanel();
        JScrollPane scroll = new JScrollPane(namesPanel);
        scroll.setPreferredSize(new Dimension(360, 220));

        JTextField[][] nameFieldsRef = new JTextField[1][];

        Runnable rebuildNames = () -> {
            int n = (Integer) countSpinner.getValue();
            namesPanel.removeAll();
            namesPanel.setLayout(new GridLayout(n, 2, 8, 8));
            JTextField[] fields = new JTextField[n];
            for (int i = 0; i < n; i++) {
                namesPanel.add(new JLabel("Person " + (i + 1) + " name:", SwingConstants.RIGHT));
                fields[i] = new JTextField("Member" + (i + 1), 14);
                namesPanel.add(fields[i]);
            }
            nameFieldsRef[0] = fields;
            namesPanel.revalidate();
            namesPanel.repaint();
        };

        countSpinner.addChangeListener(e -> rebuildNames.run());
        rebuildNames.run();

        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                parent,
                root,
                "Server setup",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        Integer port = parsePort(portField.getText());
        if (port == null) {
            JOptionPane.showMessageDialog(parent, "Port must be a number from 1 to 65535.", "Invalid port", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        JTextField[] nameFields = nameFieldsRef[0];
        int n = nameFields.length;
        String[] enteredNames = new String[n];
        for (int i = 0; i < n; i++) {
            String value = nameFields[i].getText().trim();
            if (value.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "All member names are required.", "Invalid names", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            enteredNames[i] = value;
        }
        return new ServerSetup(port, enteredNames);
    }

    private record ServerSetup(int port, String[] names) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerSetup setup = askServerSetup(null);
            if (setup == null) {
                JOptionPane.showMessageDialog(
                        null,
                        "Server start cancelled.",
                        "Setup Cancelled",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            new ServerApp(setup.port(), setup.names()).setVisible(true);
        });
    }
}
