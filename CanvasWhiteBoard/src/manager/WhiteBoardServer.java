package manager;

import whiteboard.CreateGUI;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Jiasheng Yang
 * @studentID 1464801
 * @email jiasyang@student.unimelb.edu.au
 */

public class WhiteBoardServer extends Thread {
    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;
    private List<String> usernames;
    private String username;
    private CreateGUI createGUI;
    private static int feedback;

    public WhiteBoardServer(Socket socket, List<String> usernames, CreateGUI createGUI) {
        this.socket = socket;
        this.usernames = usernames;
        this.createGUI = createGUI;
    }

    @Override
    public void run() {
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            username = input.readLine();
            if (username != null && !username.isEmpty()) {
                // System.out.println("Connected with user: " + username);
                String message;
                while ((message = input.readLine()) != null) {
                    String[] command = message.split(" ", 2);
                    // System.out.println("Received command: " + command[0]);
                    switch (command[0]) {
                        case "begin":
                            syncToUser(command[1]);
                            break;
                        case "request":
                            handleLoginRequest(command[1]);
                            break;
                        case "draw":
                            if (command[1] != null && !command[1].isEmpty()) {
                                broadcast(message);
                                repaintPanel(command[1]);
                            }
                            break;
                        case "chat":
                            broadcast(message);
                            CreateGUI.chatArea.append(command[1] + "\n");
                            break;
                        case "new":
                            createGUI.getCanvas().removeAll();
                            createGUI.getCanvas().updateUI();
                            createGUI.getCreateDrawListener().clearRecord();
                            broadcast("clear All");
                            break;
                        case "over":
                            disconnectClient();
                            break;
                        case "rejected":
                            socket.close();
                            break;
                        default:
                            output.println("Invalid Command!");
                            break;
                    }
                }
            } else {
                this.output.println("Invalid username received.");
                socket.close();
            }
        } catch (SocketException exception) {
            System.err.println("User " + username + " connection interruption.");
        } catch (IOException exception) {
            System.err.println("Error: " + exception.getMessage());
        }
    }

    private void syncToUser(String username) throws IOException {
        CopyOnWriteArrayList<String> recordList = createGUI.getCreateDrawListener().getRecords();
        if (!recordList.isEmpty()) {
            broadcastMulti(recordList);
            addUsername(username);
            createGUI.userListModel.addElement(username);
            broadcastUserList();  // broadcast the updated list
        }
    }

    public synchronized void handleLoginRequest(String currentUser) throws IOException {
        if (usernames.contains(currentUser)) {
            output.println("feedback no");
            return;
        }
        feedback = JOptionPane.showConfirmDialog(null, "Accept user: " + currentUser + " or not?", "Join Admission", JOptionPane.YES_NO_CANCEL_OPTION);
        switch (feedback) {
            case JOptionPane.YES_OPTION:
                if (addUsername(currentUser)) {
                    output.println("feedback yes");
                    broadcastUserList();  // broadcast the updated user list
                } else {
                    output.println("feedback no");
                }
                break;
            case JOptionPane.NO_OPTION:
                output.println("feedback no");
                break;
            case JOptionPane.CANCEL_OPTION:
            case JOptionPane.CLOSED_OPTION:
                output.println("feedback rejected");
                usernames.remove(currentUser);
                socket.close();
                break;
        }
    }

    private synchronized boolean addUsername(String username) {
        if (username != null && !usernames.contains(username)) {
            usernames.add(username);
            return true;
        }
        return false;
    }

    public static void kickUser(String username) throws IOException {
        WhiteBoardServer userToKick = null;
        for (WhiteBoardServer connection : CreateWhiteBoard.connections) {
            if (connection.username.equals(username)) {
                userToKick = connection;
                break;
            }
        }
        if (userToKick != null) {
            userToKick.disconnectClient();
            broadcast("kick " + username);
            String kickMessage = "chat User " + username + " has been kicked out.";
            broadcast(kickMessage);
        }
    }

    private void disconnectClient() throws IOException {
        createGUI.usernames.remove(username);
        usernames.remove(username);
        String leaveMessage = "chat User " + username + " has left.";
        broadcast(leaveMessage); // broadcast the leave message

        String remainClient = "clientout " + username + " " + String.join(" ", createGUI.usernames);
        if (CreateWhiteBoard.connections == null) {
            throw new NullPointerException("CreateWhiteBoard.connections is not initialized");
        }
        for (WhiteBoardServer connection : CreateWhiteBoard.connections) {
            if (connection.output != null) {
                connection.output.println(remainClient);
            } else {
                System.err.println("Connection output is null for a connection");
            }
        }
        SwingUtilities.invokeLater(() -> {
            CreateGUI.chatArea.append("User " + username + " has left.\n");
            createGUI.userListModel.removeElement(username);
            String[] parts = remainClient.split(" ", 3);
            if (parts.length > 2) {
                String[] clientList = parts[2].split(" ");
                CreateGUI.list.setListData(clientList);
            } else {
                CreateGUI.list.setListData(new String[0]);
            }
        });
        broadcastUserList(); // rroadcast updated user list after client disconnect
    }

    private void broadcastUserList() throws IOException {
        String userListMessage = "userList " + String.join(";", usernames);
        for (WhiteBoardServer connection : CreateWhiteBoard.connections) {
            connection.output.println(userListMessage);
        }
    }

    public static void broadcastMulti(CopyOnWriteArrayList<String> recordList) throws IOException {
        for (String record : recordList) {
            for (WhiteBoardServer connection : CreateWhiteBoard.connections) {
                connection.output.println("draw " + record);
            }
        }
    }

    public static void broadcast(String message) throws IOException {
        // System.out.println("broadcast message: " + message);
        for (WhiteBoardServer connection : CreateWhiteBoard.connections) {
            connection.output.println(message);
        }
    }

    public void repaintPanel(String drawRecord) {
        createGUI.getCreateDrawListener().update(drawRecord);
        createGUI.getCanvas().repaint();
    }
}
