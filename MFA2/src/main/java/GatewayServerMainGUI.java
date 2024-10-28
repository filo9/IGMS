import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.io.PrintStream;

public class GatewayServerMainGUI {
    private JFrame frame;
    private JTextArea logArea;
    private JButton toggleServersButton;  // 按钮声明为类的成员变量

    // 服务线程
    private Thread adminThread, gatewayServerThread, gatewayServeriiThread;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GatewayServerMainGUI().createAndShowGUI());
    }

    private void createAndShowGUI() {
        frame = new JFrame("网关服务器管理");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setUndecorated(true);

        // 重定向 System.out 和 System.err
        redirectSystemStreams();

        // 设置渐变背景
        frame.setContentPane(new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gradientPaint = new GradientPaint(
                        0, 0, new Color(64, 64, 64),  // 暗灰色
                        0, getHeight(), Color.BLACK); // 渐变到黑色
                g2d.setPaint(gradientPaint);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        });

        frame.setLayout(new BorderLayout());

        // 日志显示区域
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.WHITE);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        // JScrollPane 设置
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());  // 移除默认边框
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER); // 移除垂直滚动条
        scrollPane.setPreferredSize(new Dimension(600, 300)); // 设置日志区域的大小
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controlPanel.setBackground(new Color(64, 64, 64));  // 暗灰色背景
        controlPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));  // 设置边框为零

        // 创建按钮（切换启动/停止状态）
        toggleServersButton = createButton("启动", e -> toggleServers(),
                new Color(173, 216, 230), Color.WHITE);
        controlPanel.add(toggleServersButton);

        frame.add(controlPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        makeFrameDraggable();  // 使窗口可拖动
    }

    private JButton createButton(String text, ActionListener actionListener, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.addActionListener(actionListener);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        button.setBorder(BorderFactory.createRaisedBevelBorder());

        // 设置按钮尺寸
        button.setPreferredSize(new Dimension(80, 60));
        return button;
    }

    private void toggleServers() {
        if (toggleServersButton.getText().equals("启动")) {
            startAllServers();
            toggleServersButton.setText("停止");
            toggleServersButton.setBackground(new Color(211, 211, 211));
            toggleServersButton.setForeground(new Color(255, 102, 102));
        } else {
            stopAllServers();
            toggleServersButton.setText("启动");
            toggleServersButton.setBackground(new Color(173, 216, 230));
            toggleServersButton.setForeground(Color.WHITE);
        }
    }

    private void startAllServers() {
        if (adminThread == null || !adminThread.isAlive()) {
            adminThread = new Thread(() -> {
                try {
                    log("启动 Administrator...");
                    Administrator.main(null);
                    log("Administrator 已启动");
                } catch (Exception e) {
                    log("Administrator 启动失败: " + e.getMessage());
                }
            });
            adminThread.start();
        }

        if (gatewayServerThread == null || !gatewayServerThread.isAlive()) {
            gatewayServerThread = new Thread(() -> {
                try {
                    log("启动 GatewayServer...");
                    GatewayServer.main(null);
                    log("GatewayServer 已启动");
                } catch (Exception e) {
                    log("GatewayServer 启动失败: " + e.getMessage());
                }
            });
            gatewayServerThread.start();
        }

        if (gatewayServeriiThread == null || !gatewayServeriiThread.isAlive()) {
            gatewayServeriiThread = new Thread(() -> {
                try {
                    log("启动 GatewayServerii...");
                    GatewayServerii.main(null);
                    log("GatewayServerii 已启动");
                } catch (Exception e) {
                    log("GatewayServerii 启动失败: " + e.getMessage());
                }
            });
            gatewayServeriiThread.start();
        }
    }

    private void stopAllServers() {
        System.exit(0);  // 停止所有服务并退出程序
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength()); // 自动滚动到底部
        });
    }

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                logArea.append(String.valueOf((char) b));
                logArea.setCaretPosition(logArea.getDocument().getLength()); // 自动滚动到底部
            }

            @Override
            public void write(byte[] b, int off, int len) {
                logArea.append(new String(b, off, len));
                logArea.setCaretPosition(logArea.getDocument().getLength()); // 自动滚动到底部
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    private Point initialClick;

    private void makeFrameDraggable() {
        frame.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                initialClick = e.getPoint();
            }
        });

        frame.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                // 获取当前窗口的位置
                int thisX = frame.getLocation().x;
                int thisY = frame.getLocation().y;

                // 计算新的位置
                int newX = thisX + e.getX() - initialClick.x;
                int newY = thisY + e.getY() - initialClick.y;

                // 设置新位置
                frame.setLocation(newX, newY);
            }
        });
    }
}
