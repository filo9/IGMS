import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.io.PrintStream;

public class GatewayServerMainGUI {
    private JFrame frame;
    private JTextArea logArea;
    private JButton startServersButton, stopServersButton;

    // 服务线程
    private Thread adminThread, gatewayServerThread, gatewayServeriiThread;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GatewayServerMainGUI().createAndShowGUI());
    }

    private void createAndShowGUI() {
        // 创建主窗口
        frame = new JFrame("网关服务器管理");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        // 设置日志输出重定向
        redirectSystemStreams();

        // 设置渐变背景
        frame.setContentPane(new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gradientPaint = new GradientPaint(0, 0, Color.LIGHT_GRAY, 0, getHeight(), Color.WHITE);
                g2d.setPaint(gradientPaint);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        });
        frame.setLayout(new BorderLayout());

        // 日志显示区域
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // 控制面板
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(1, 2, 10, 10));

        // 启动按钮: 淡蓝色背景，白色字体
        startServersButton = createButton("启动所有服务器", e -> startAllServers(), new Color(173, 216, 230), Color.WHITE);
        // 停止按钮: 灰色背景，黄色字体
        stopServersButton = createButton("停止所有服务器", e -> stopAllServers(), Color.GRAY, Color.YELLOW);

        controlPanel.add(startServersButton);
        controlPanel.add(stopServersButton);

        frame.add(controlPanel, BorderLayout.SOUTH);

        // 设置窗口居中
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // 创建按钮的通用方法（增加了背景色和字体色参数）
    private JButton createButton(String text, ActionListener actionListener, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.addActionListener(actionListener);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        return button;
    }

    // 日志输出
    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    // 重定向System.out和System.err到logArea
    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                logArea.append(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) {
                logArea.append(new String(b, off, len));
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    // 启动所有服务
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

    // 停止所有服务
    private void stopAllServers() {
        System.exit(0);  // 停止所有服务并退出程序
    }
}