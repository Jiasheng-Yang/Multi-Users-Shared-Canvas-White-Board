package whiteboard;

import configurations.Config;
import manager.WhiteBoardServer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Jiasheng Yang
 * @studentID 1464801
 * @email jiasyang@student.unimelb.edu.au
 */

public class CreateGUI extends JFrame {
    public static JTextArea chatArea;
    public CopyOnWriteArrayList<String> usernames = new CopyOnWriteArrayList<>();
    public static JTextField chatInput;
    public DefaultListModel<String> userListModel = new DefaultListModel<>();
    public PaintListener createDrawListener;
    public static JList<String> list;
    public CanvasPanel canvas;
    public static CopyOnWriteArrayList<String> recordList = new CopyOnWriteArrayList<>();
    public String currentUser;

    public CreateGUI(String username) {
        currentUser = username;
        setTitle("Whiteboard - " + username);
        setSize(1280, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initializeUI(username);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    WhiteBoardServer.broadcast("shutdown");
                } catch (IOException exception) {
                    System.err.println("Error broadcasting shutdown: " + exception.getMessage());
                    exception.printStackTrace();
                }
                System.exit(0);
            }
        });
    }

    private void initializeUI(String currentUser) {
        setLayout(new BorderLayout());

        createDrawListener = new PaintListener(this, recordList);

        // menu bar
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // file menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenuItem newMenuItem = new JMenuItem("New");
        JMenuItem openMenuItem = new JMenuItem("Open");
        JMenuItem saveMenuItem = new JMenuItem("Save");
        JMenuItem saveAsMenuItem = new JMenuItem("Save As");
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(exitMenuItem);

        newMenuItem.addActionListener(e -> {
            clearPanel();
            try {
                WhiteBoardServer.broadcast("clear All");
            } catch (IOException exception) {
                System.err.println("Error in the new white board: " + exception.getMessage());
            }
        });

        openMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(CreateGUI.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    try {
                        // WhiteBoardServer.broadcast("clear All");
                        canvas.loadCanvas(selectedFile.getAbsolutePath());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(CreateGUI.this, "Error loading canvas", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        saveMenuItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showSaveDialog(canvas);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    canvas.saveCanvas(file);
                    JOptionPane.showMessageDialog(canvas, "original saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException exception) {
                    exception.printStackTrace();
                    JOptionPane.showMessageDialog(canvas, "Error saving file", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        saveAsMenuItem.addActionListener(e -> {
            JDialog saveAsDialog = new JDialog((JFrame) null, "Save", true);
            saveAsDialog.setLayout(new BoxLayout(saveAsDialog.getContentPane(), BoxLayout.Y_AXIS));
            JPanel filename = new JPanel();
            filename.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
            JLabel nameLabel = new JLabel("Save as:");
            filename.add(nameLabel);
            JTextField nameField = new JTextField("white_board", 13);
            filename.add(nameField);
            String[] formats = {".jpg", ".png"};
            JComboBox<String> formatComboBox = new JComboBox<>(formats);
            filename.add(formatComboBox);
            saveAsDialog.add(filename);
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
            JButton saveButton = new JButton("OK");
            saveButton.addActionListener(saveEvent -> {
                String fileName = nameField.getText();
                String format = (String) formatComboBox.getSelectedItem();
                if (fileName != null && !fileName.trim().isEmpty() && format != null) {
                    BufferedImage image = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = image.createGraphics();
                    canvas.paint(g2d);
                    g2d.dispose();
                    try {
                        System.out.println("Saving to: " + fileName + format);
                        ImageIO.write(image, format.substring(1), new File(fileName + format));
                        JOptionPane.showMessageDialog(saveAsDialog, "Image saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException exception) {
                        System.err.println("Error saving image: " + exception.getMessage());
                        JOptionPane.showMessageDialog(saveAsDialog, "Error saving image: " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    saveAsDialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(saveAsDialog, "Please enter a valid file name and select a format.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            buttonPanel.add(saveButton);
            saveAsDialog.add(buttonPanel);
            saveAsDialog.setSize(350, 110);
            saveAsDialog.setLocationRelativeTo(null);
            saveAsDialog.setVisible(true);
        });

        exitMenuItem.addActionListener(e -> {
            try {
                WhiteBoardServer.broadcast("shutdown");
            } catch (IOException exception) {
                System.err.println("Error! " + exception.getMessage());
            }
            System.exit(0);
        });

        // toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);
        // tool buttons
        String[] toolNames = {"Pen", "Eraser", "Line", "Rectangle", "Circle", "Oval", "Text"};
        for (String toolName : toolNames) {
            JButton button = new JButton(toolName);
            button.setActionCommand(toolName);
            button.addActionListener(createDrawListener);
            toolBar.add(button);
        }

        // thickness
        JComboBox<String> sizeComboBox = new JComboBox<String>(new String[]{"4", "7", "10"}) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 30);
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        sizeComboBox.addActionListener(e -> createDrawListener.thickness = Integer.parseInt((String) sizeComboBox.getSelectedItem()));
        toolBar.add(sizeComboBox);

        // fonttype
        JComboBox<String> fontComboBox = new JComboBox<String>(new String[]{"Arial", "Serif", "Verdana"}) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 30);
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        fontComboBox.addActionListener(e -> createDrawListener.setFontType((String) fontComboBox.getSelectedItem()));
        toolBar.add(fontComboBox);

        // color chosen button
        JButton colorButton = new JButton("Color");
        colorButton.addActionListener(e -> {
            Color curColor = JColorChooser.showDialog(CreateGUI.this, "Choose color", null);
            if (curColor != null) {
                createDrawListener.setColor(curColor);
            }
        });
        toolBar.add(colorButton);

        // draw panel
        canvas = new CanvasPanel(recordList);
        canvas.setBackground(Color.WHITE);
        canvas.setBorder(null);
        canvas.addMouseListener(createDrawListener);
        canvas.addMouseMotionListener(createDrawListener);
        add(canvas, BorderLayout.CENTER);

        // user list
        JPanel userListPanel = new JPanel(new BorderLayout());
        userListPanel.setBorder(BorderFactory.createTitledBorder("Active Users"));
        if (this.currentUser.contains("Manager")) {
            userListModel.addElement("Manager: " + currentUser);
        } else {
            userListModel.addElement(this.currentUser);
        }
        JList<String> userList = new JList<>(userListModel);
        userListPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        JButton kickOutButton = new JButton("Kick Out");
        kickOutButton.addActionListener(e -> {
            int selectedIndex = userList.getSelectedIndex();
            if (selectedIndex != -1) {
                String user = userList.getSelectedValue();
                userListModel.remove(selectedIndex);
                try {
                    WhiteBoardServer.kickUser(user);
                } catch (IOException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error sending message: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
                chatArea.append("User " + user + " has been kicked out\n");
            } else {
                JOptionPane.showMessageDialog(null, "No user selected", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        userListPanel.add(kickOutButton, BorderLayout.SOUTH);
        userListPanel.setPreferredSize(new Dimension(300, 300));

        // chat
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat Room"));
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        JPanel chatInputPanel = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        JButton sendButton = new JButton("Send");
        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);
        chatPanel.setPreferredSize(new Dimension(300, 200));
        sendButton.addActionListener(e -> talk(currentUser));
        chatInput.addActionListener(e -> talk(currentUser));

        JSplitPane rightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, userListPanel, chatPanel);
        rightPane.setDividerLocation(300);
        add(rightPane, BorderLayout.EAST);
    }

    private void clearPanel() {
        Graphics g = canvas.getGraphics();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.clearCanvas();
        canvas.repaint();
    }

    private void talk(String currentUser) {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            String formattedMessage = currentUser + ": " + message;
            chatArea.append(formattedMessage + "\n");
            chatInput.setText("");
            try {
                WhiteBoardServer.broadcast("chat " + formattedMessage);
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(null, "Error sending message: " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public CanvasPanel getCanvas() {
        return canvas;
    }

    public PaintListener getCreateDrawListener() {
        return createDrawListener;
    }

    // Inner CanvasPanel class
    public class CanvasPanel extends JPanel {
        private CopyOnWriteArrayList<String> recordList;

        public CanvasPanel(CopyOnWriteArrayList<String> recordList) {
            this.recordList = recordList;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            draw((Graphics2D) g);
        }

        private void draw(Graphics2D gr) {
            for (String line : recordList) {
                String[] record = line.split(";");
                try {
                    String toolType = record[0];
                    int thickness = Integer.parseInt(record[1]);
                    Color color = new Color(
                            Integer.parseInt(record[2]), // Red
                            Integer.parseInt(record[3]), // Green
                            Integer.parseInt(record[4])  // Blue
                    );
                    gr.setColor(color);
                    gr.setStroke(new BasicStroke(thickness));

                    int startX = Integer.parseInt(record[5]);
                    int startY = Integer.parseInt(record[6]);

                    if (toolType.equals("Text")) {
                        String text = record[7];
                        Font f = new Font(record[8], Font.PLAIN, thickness + 10);
                        gr.setFont(f);
                        gr.drawString(text, startX, startY);
                    } else {
                        int endX = Integer.parseInt(record[7]);
                        int endY = Integer.parseInt(record[8]);
                        switch (toolType) {
                            case "Circle":
                            case "Oval":
                                drawEllipse(gr, toolType, startX, startY, endX, endY);
                                break;
                            case "Rectangle":
                                drawRectangle(gr, startX, startY, endX, endY);
                                break;
                            case "Line":
                                gr.drawLine(startX, startY, endX, endY);
                                break;
                            case "Eraser":
                                gr.drawLine(startX, startY, endX, endY);
                        }
                    }
                } catch (NumberFormatException exception) {
                    System.err.println("Error parsing integer values: " + exception.getMessage());
                    JOptionPane.showMessageDialog(null, "Error! Invalid drawing parameters: " + exception.getMessage(), "Drawing Error", JOptionPane.ERROR_MESSAGE);
                } catch (IllegalArgumentException exception) {
                    System.err.println("Error! Invalid drawing parameters: " + exception.getMessage());
                    JOptionPane.showMessageDialog(null, "Error! Invalid drawing parameters: " + exception.getMessage(), "Drawing Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception exception) {
                    System.err.println("Error! " + exception.getMessage());
                    JOptionPane.showMessageDialog(null, "Error! " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void drawEllipse(Graphics2D g, String type, int x1, int y1, int x2, int y2) {
            int width = Math.abs(x1 - x2);
            int height = Math.abs(y1 - y2);
            if (type.equals("Circle")) {
                int diameter = Math.min(width, height);
                g.drawOval(Math.min(x1, x2), Math.min(y1, y2), diameter, diameter);
            } else {
                g.drawOval(Math.min(x1, x2), Math.min(y1, y2), width, height);
            }
        }

        private void drawRectangle(Graphics2D g, int x1, int y1, int x2, int y2) {
            g.drawRect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2));
        }

        @Override
        public void removeAll() {
            super.removeAll();
        }

        @Override
        public void updateUI() {
            super.updateUI();
        }

        public void clearCanvas() {
            recordList.clear();
            repaint();
        }

        // save canvas
        public void saveCanvas(File file) throws IOException {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(recordList);
            }
        }

        public void loadCanvas(String filePath) throws IOException {
            System.out.println("Clearing canvas and loading new content from file: " + filePath);
            // clear current canvas
            getCanvas().removeAll();
            getCanvas().updateUI();
            getCreateDrawListener().clearRecord();

            // load and broadcast
            File file = new File(filePath);
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                CopyOnWriteArrayList<String> recordList = (CopyOnWriteArrayList<String>) ois.readObject();
                for (String record : recordList) {
                    getCreateDrawListener().update(record);
                }
                getCanvas().repaint();
                WhiteBoardServer.broadcast("clear All");
                WhiteBoardServer.broadcastMulti(recordList);
            } catch (ClassNotFoundException exception) {
                System.err.println("Error loading canvas: " + exception.getMessage());
                JOptionPane.showMessageDialog(this, "Error loading canvas", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

    }

    // Inner PaintListener class
    public class PaintListener implements ActionListener, MouseListener, MouseMotionListener {
        Graphics2D g;
        JFrame frame;
        int thickness = Config.THICKNESS;
        Color color = Config.COLOR;
        String toolType = Config.TOOLTYPE.toString();
        String fontType = Config.FONTTYPE.toString();
        int startX, startY, endX, endY;
        String record;
        CopyOnWriteArrayList<String> recordList;

        public PaintListener(JFrame frame, CopyOnWriteArrayList<String> recordList) {
            this.frame = frame;
            this.recordList = recordList;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public void setToolCursor(String toolType) {
            this.toolType = toolType;

            Cursor cursor;
            switch (toolType) {
                case "Pen":
                case "Text":
                    cursor = Cursor.getDefaultCursor();
                    break;
                case "Oval":
                case "Circle":
                case "Rectangle":
                case "Line":
                    cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
                    break;
                default:
                    cursor = Cursor.getDefaultCursor();
                    break;
            }
            frame.setCursor(cursor);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setToolCursor(e.getActionCommand());
        }

        @Override
        public void mousePressed(MouseEvent e) {
            startX = e.getX();
            startY = e.getY();
            if (g == null) {
                g = (Graphics2D) canvas.getGraphics();
            }
            if (!g.getColor().equals(color)) {
                g.setColor(color);
            }
            // record = null;
            if (toolType.equals("Pen")) {
                record = drawObject(color, startX, startY, startX, startY, this.thickness);
                recordList.add(record);
                try {
                    WhiteBoardServer.broadcast("draw " + record);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            endX = e.getX();
            endY = e.getY();
            // record = null;
            try {
                switch (toolType) {
                    case "Line":
                        configGraphics();
                        g.drawLine(startX, startY, endX, endY);
                        record = generateRecord("Line", startX, startY, endX, endY);
                        break;
                    case "Circle":
                        configGraphics();
                        int diameter = Math.max(Math.abs(endX - startX), Math.abs(endY - startY));
                        g.drawOval(startX - diameter, startY - diameter, 2 * diameter, 2 * diameter);
                        record = generateRecord("Circle", startX - diameter, startY - diameter, 2 * diameter, 2 * diameter);
                        break;
                    case "Oval":
                        configGraphics();
                        int width = Math.abs(endX - startX);
                        int height = Math.abs(endY - startY);
                        g.drawOval(Math.min(startX, endX), Math.min(startY, endY), width, height);
                        record = generateRecord("Oval", startX, startY, endX, endY);
                        break;
                    case "Rectangle":
                        configGraphics();
                        g.drawRect(Math.min(startX, endX), Math.min(startY, endY), Math.abs(startX - endX), Math.abs(startY - endY));
                        record = generateRecord("Rectangle", startX, startY, endX, endY);
                        break;
                    case "Text":
                        String text = JOptionPane.showInputDialog("Please Enter Input Text");
                        if (text != null && !text.isEmpty()) {
                            Font f = new Font(fontType, Font.PLAIN, this.thickness + 10);
                            g.setFont(f);
                            g.drawString(text, startX, startY);
                            record = "Text;" + this.thickness + ";" + getColorToString(color) + ";" + startX + ";" + startY + ";" + text + ";" + fontType;
                        }
                        break;
                }
                if (record != null & !record.isEmpty()) {
                    recordList.add(record);
                    WhiteBoardServer.broadcast("draw " + record);
                }
            } catch (IllegalArgumentException exception) {
                System.err.println("Error! Invalid drawing parameters: " + exception.getMessage());
                JOptionPane.showMessageDialog(null, "Error! Invalid drawing parameters: " + exception.getMessage(), "Drawing Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception exception) {
                System.err.println("Error! " + exception.getMessage());
                JOptionPane.showMessageDialog(null, "Error! " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            int currentX = e.getX();
            int currentY = e.getY();
            try {
                if (toolType.equals("Pen")) {
                    record = drawObject(color, startX, startY, currentX, currentY, this.thickness);
                    recordList.add(record);
                    startX = currentX;
                    startY = currentY;
                } else if (toolType.equals("Eraser")) {
                    g.setColor(Color.white);
                    record = drawObject(Color.white, startX, startY, currentX, currentY, this.thickness * 2);
                    recordList.add(record);
                    startX = currentX;
                    startY = currentY;
                }
                WhiteBoardServer.broadcast("draw " + record);
            } catch (IllegalArgumentException exception) {
                System.err.println("Error! Invalid drawing parameters: " + exception.getMessage());
                JOptionPane.showMessageDialog(null, "Error! Invalid drawing parameters: " + exception.getMessage(), "Drawing Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception exception) {
                System.err.println("Error! " + exception.getMessage());
                JOptionPane.showMessageDialog(null, "Error! " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private String getColorToString(Color color) {
            return color.getRed() + ";" + color.getGreen() + ";" + color.getBlue();
        }

        public void setFontType(String fontType) {
            this.fontType = fontType;
        }

        private void configGraphics() {
            g.setStroke(new BasicStroke(thickness));
            g.setColor(color);
        }

        public String drawObject(Color color, int x1, int y1, int x2, int y2, int thickness) {
            g.setStroke(new BasicStroke(thickness));
            g.drawLine(x1, y1, x2, y2);
            return "Line;" + thickness + ";" + getColorToString(color) + ";" + x1 + ";" + y1 + ";" + x2 + ";" + y2;
        }

        private String generateRecord(String shape, int x1, int y1, int x2, int y2) {
            return String.format("%s;%d;%s;%d;%d;%d;%d", shape, thickness, getColorToString(color), x1, y1, x2, y2);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }

        public void update(String drawRecord) {
            recordList.add(drawRecord);
            frame.repaint();
        }

        public CopyOnWriteArrayList<String> getRecords() {
            return new CopyOnWriteArrayList<>(recordList);
        }

        public void clearRecord() {
            recordList.clear();
            frame.repaint();
        }
    }
}
