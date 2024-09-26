package manager;

import whiteboard.CreateGUI;
import whiteboard.Login;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Jiasheng Yang
 * @studentID 1464801
 * @email jiasyang@student.unimelb.edu.au
 */

public class CreateWhiteBoard {
    public static List<WhiteBoardServer> connections = new ArrayList<>();
    public static List<String> usernames = Collections.synchronizedList(new ArrayList<>());  // synchronized list
    private static int clientId = 0;
    private static CreateGUI create;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new Login(new Login.Callback() {
            private JFrame loginFrame;
            private String username;
            private String serverAddress;
            private int port;

            @Override
            public void loginInfo(String serverAddress, int port, String username, JFrame loginFrame) {
                this.serverAddress = serverAddress;
                this.port = port;
                this.username = username;
                this.loginFrame = loginFrame;
            }

            @Override
            public void connect() {
                new Thread(() -> {
                    startServer(port, username, loginFrame);
                }, "ServerThread").start();
            }
        }));
    }

    public static void startServer(int port, String username, JFrame loginFrame) {
        usernames.add(username);
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("CreateWhiteBoard started on port: " + port + " with Manager: " + username);

            // CreateGUI window for the manager
            EventQueue.invokeLater(() -> {
                create = new CreateGUI(username);
                create.setVisible(true);
                loginFrame.dispose();
            });

            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientId++;
                System.out.println("Connection '" + clientId + "' from " + clientSocket.getInetAddress().getHostAddress());
                WhiteBoardServer connection = new WhiteBoardServer(clientSocket, usernames, create);
                connections.add(connection);
                connection.start();
            }
        } catch (IOException e) {
            System.err.println("Error! Failed to connect the CreateWhiteBoard: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to start server on port: " + port, "CreateWhiteBoard Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public static synchronized List<String> getUsernames() {
        return new ArrayList<>(usernames);
    }

    public void CreateWhiteBoard() {
    }
}
