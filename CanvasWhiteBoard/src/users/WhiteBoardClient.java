package users;

import whiteboard.JoinGUI;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

/**
 * @author Jiasheng Yang
 * @studentID 1464801
 * @email jiasyang@student.unimelb.edu.au
 */

public class WhiteBoardClient {
    Socket socket;
    BufferedReader input;
    public PrintWriter output;
    private volatile String status = "wait";
    private CountDownLatch latch;
    private String currentUser;

    public WhiteBoardClient(Socket socket, CountDownLatch latch, String currentUser) throws IOException {
        this.socket = socket;
        this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.output = new PrintWriter(socket.getOutputStream(), true);
        this.latch = latch;
        this.currentUser = currentUser;
    }

    public void startConnection() {
        try {
            System.out.println("Connection started.");
            String message;
            while ((message = this.input.readLine()) != null) {
                // System.out.println("received: " + message);
                String[] command = message.split(" ", 2);
                // System.out.println("command received: " + command[0]);
                // System.out.println("para: " + command[1]);
                switch (command[0]) {
                    case "feedback":
                        updateStatus(command[1]);
                        latch.countDown(); // notify main thread about the status update
                        break;
                    case "chat":
                        JoinGUI.chatArea.append(command[1] + "\n");
                        break;
                    case "userList":
                        String[] userList = command[1].split(";");
                        JoinGUI.userListModel.clear();
                        for (String user : userList) {
                            JoinGUI.userListModel.addElement(user);
                        }
                        break;
                    case "draw":
                        if (command[1] != null && !command[1].isEmpty()) {
                            JoinGUI.recordList.add(command[1]);
                            JoinGUI.getCanvas().repaint();
                        }
                        break;
                    case "kick":
                        if (command.length > 1 && currentUser.equals(command[1])) {
                            JOptionPane.showMessageDialog(null, "You have been kicked out!", "Kicked Out", JOptionPane.INFORMATION_MESSAGE);
                            System.exit(0);
                        }
                        break;
                    case "clear":
                        JoinGUI.canvas.clearCanvas();
                        break;
                    case "shutdown":
                        System.out.println("socket shut down");
                        JOptionPane.showMessageDialog(JoinGUI.frame, "The manager leaves the canvas, the socket is shutdown, please close the window");
                        socket.close();
                        System.exit(0);
                        break;
                    case "clientout":
                        String[] users = command[1].split(" ", 2);
                        String[] userListUpdate = users[1].split(" ");
                        JoinGUI.userListModel.clear();
                        for (String user : userListUpdate) {
                            JoinGUI.userListModel.addElement(user);
                        }
                        break;
                    default:
                        System.out.println("Unknown command: " + command[0]);
                }
            }
        } catch (IOException exception) {
            System.exit(0);
        }
    }

    /*
    private void handleOpenFile(String filePath) {
        SwingUtilities.invokeLater(() -> {
            try {
                File file = new File(filePath);
                JoinGUI.canvas.loadCanvas(file);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(JoinGUI.frame, "Error loading file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

     */
    private synchronized void updateStatus(String newStatus) {
        this.status = newStatus;
    }

    public synchronized String getStatus() {
        return status;
    }

    public synchronized void resetStatus() {
        this.status = "wait";
    }
}
