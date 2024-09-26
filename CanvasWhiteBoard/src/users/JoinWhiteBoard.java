package users;

import whiteboard.JoinGUI;
import whiteboard.Login;

import javax.swing.*;
import java.awt.*;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Jiasheng Yang
 * @studentID 1464801
 * @email jiasyang@student.unimelb.edu.au
 */

public class JoinWhiteBoard {
    public static WhiteBoardClient connection;

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
                EventQueue.invokeLater(loginFrame::dispose);
                new Thread(() -> {
                    new JoinWhiteBoard(serverAddress, port, username, loginFrame);
                }).start();
            }
        }));
    }

    public JoinWhiteBoard(String serverAddress, int port, String username, JFrame loginFrame) {
        connectServer(serverAddress, port, username, loginFrame);
    }

    public static void connectServer(String serverAddress, int port, String username, JFrame loginFrame) {
        try {
            Socket socket = new Socket(serverAddress, port);
            CountDownLatch latch = new CountDownLatch(1);
            connection = new WhiteBoardClient(socket, latch, username);

            connection.output.println(username);
            connection.output.println("request " + username);

            new Thread(connection::startConnection).start();
            latch.await(30, TimeUnit.SECONDS); // wait for the status update up to 30 seconds

            String feedback = connection.getStatus();
            System.out.println("Feedback: " + feedback);

            switch (feedback) {
                case "yes":
                    JOptionPane.showMessageDialog(loginFrame, "Login Succeed!");
                    loginFrame.dispose();
                    SwingUtilities.invokeLater(() -> {
                        JoinGUI join = new JoinGUI(username);
                        join.setVisible(true);
                        connection.output.println("begin " + username);
                    });
                    break;
                case "no":
                    JOptionPane.showMessageDialog(loginFrame, "Login Failed, Username exists!", "User Taken", JOptionPane.ERROR_MESSAGE);
                    connection.resetStatus();
                    break;
                case "rejected":
                    JOptionPane.showMessageDialog(loginFrame, "Login Failed, Rejected by manager!", "Rejected", JOptionPane.ERROR_MESSAGE);
                    connection.output.println("rejected");
                    break;
                default:
                    JOptionPane.showMessageDialog(loginFrame, "Login Failed, Timeout!", "Timeout", JOptionPane.ERROR_MESSAGE);
                    break;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
