package whiteboard;

import configurations.Config;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener;

/**
 * @author Jiasheng Yang
 * @studentID 1464801
 * @email jiasyang@student.unimelb.edu.au
 */

public class Login {
    static String serverAddress = Config.SERVER_ADDRESS;
    static int port = Config.PORT;
    static String manager = Config.MANAGER_NAME;
    static String guest = Config.USER_NAME;

    public interface Callback {
        void loginInfo(String serverAddress, int port, String username, JFrame loginFrame);
        void connect();
    }

    private static Callback callback;

    public Login(Callback callback) {
        this.callback = callback;
        initialize();
    }

    public static void initialize() {
        JFrame loginFrame = new JFrame("Whiteboard Login");
        loginFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        loginFrame.setSize(320, 300);
        loginFrame.setLayout(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
            System.exit(0);
            }
        });


        // selected buttons for creating or joining a whiteboard
        JRadioButton create = new JRadioButton("Create a new whiteboard", true);
        JRadioButton join = new JRadioButton("Join a whiteboard");
        create.setBounds(20, 20, 200, 30);
        join.setBounds(20, 50, 200, 30);
        ButtonGroup group = new ButtonGroup();
        group.add(create);
        group.add(join);
        loginFrame.add(create);
        loginFrame.add(join);

        // server IP
        JLabel ipLabel = new JLabel("IP: ");
        JTextField ipField = new JTextField(serverAddress);
        ipLabel.setBounds(20, 90, 80, 30);
        ipField.setBounds(120, 90, 160, 30);
        loginFrame.add(ipLabel);
        loginFrame.add(ipField);

        // port
        JLabel portLabel = new JLabel("Port: ");
        JTextField portField = new JTextField(String.valueOf(port));
        portLabel.setBounds(20, 130, 80, 30);
        portField.setBounds(120, 130, 160, 30);
        loginFrame.add(portLabel);
        loginFrame.add(portField);

        // username
        JLabel usernameLabel = new JLabel("Username: ");
        JTextField usernameField = new JTextField(manager);
        usernameLabel.setBounds(20, 170, 80, 30);
        usernameField.setBounds(120, 170, 160, 30);
        loginFrame.add(usernameLabel);
        loginFrame.add(usernameField);


        // select operation to change default username
        create.addActionListener(e -> {
            usernameLabel.setText("Manager Username: ");
            usernameField.setText(manager);
        });
        join.addActionListener(e -> {
            usernameLabel.setText("Guest Username: ");
            usernameField.setText(guest);
        });

        JButton loginButton = new JButton("LOGIN");
        loginButton.setBounds(20, 220, 100, 30);
        loginButton.addActionListener(e -> {
            String serverAddress = ipField.getText();
            int port = Integer.parseInt(portField.getText());
            String username = usernameField.getText();
            boolean isCreate = create.isSelected();
            boolean isJoin = join.isSelected();
            // CreateWhiteBoard.startServer(port, username, loginFrame);

            callback.loginInfo(serverAddress, port, username, loginFrame);
            callback.connect();
            try {
                if (isCreate) {
                    System.out.println("Creating a new whiteboard...");
                    // CreateWhiteBoard.startServer(port, username, loginFrame);
                } else if (isJoin) {
                    System.out.println("Joining an existing whiteboard...");
                    // JoinWhiteBoard.connectServer(serverAddress, port, username, loginFrame);
                }
                EventQueue.invokeLater(() -> {
                    loginFrame.dispose();
                    System.out.println("Login frame is disposed");
                });
            } catch (NumberFormatException exception) {
                System.err.println("Invalid port number! " + exception.getMessage());
                JOptionPane.showMessageDialog(loginFrame, "Invalid port number! " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception exception) {
                System.err.println("Error! " + exception.getMessage());
                JOptionPane.showMessageDialog(loginFrame, "Error! " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }

        });

        loginFrame.add(loginButton);

        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    }
}
