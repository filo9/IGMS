import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class User1 extends JFrame {
    private static String DEVICE_NAME = USER.USER_NAME;
    private static String TARGET_DEVICE_NAME = "ElectronicDoor";
    private Point initialClick;

    public User1() {
        setTitle("门控制 - 用户：" + USER.USER_NAME);
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 居中显示
        setUndecorated(true); // 设置无边框

        // 创建主面板
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1, 10, 10)); // 4行1列布局
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 内边距

        // 添加按钮
        JButton openButton = new JButton("开门");
        JButton closeButton = new JButton("关门");
        JButton changePasswordButton = new JButton("更改密码");
        JButton exitButton = new JButton("退出");

        // 为每个按钮添加事件监听器
        openButton.addActionListener(e -> sendCommand("OPEN"));
        closeButton.addActionListener(e -> sendCommand("CLOSE"));
        changePasswordButton.addActionListener(e -> changePassword());
        exitButton.addActionListener(e -> {
            dispose(); // 关闭当前窗口
            SwingUtilities.invokeLater(USER::new); // 重新打开USER界面
        });

        // 将按钮添加到面板
        panel.add(openButton);
        panel.add(closeButton);
        panel.add(changePasswordButton);
        panel.add(exitButton);

        // 将面板添加到窗口
        add(panel);

        // 添加鼠标事件来实现窗口拖动
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
                getComponentAt(initialClick);
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // 获取当前窗口位置
                int thisX = getLocation().x;
                int thisY = getLocation().y;

                // 计算新位置
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;

                // 设置窗口新位置
                int X = thisX + xMoved;
                int Y = thisY + yMoved;
                setLocation(X, Y);
            }
        });

        setVisible(true);
    }

    private void sendCommand(String command) {
        Client.setCommand(command);
        Client.setDeviceName(DEVICE_NAME);
        Client.setTargetDeviceName(TARGET_DEVICE_NAME);
        Client.main(null); // 启动TargetDevice
    }

    private void changePassword() {
        String newPassword = null;
        while (true) {
            String firstPassword = JOptionPane.showInputDialog(this, "请输入新的8位密码：");
            if (firstPassword == null) return; // 用户取消操作

            String secondPassword = JOptionPane.showInputDialog(this, "请再次输入新的8位密码：");
            if (secondPassword == null) return; // 用户取消操作

            if (firstPassword.equals(secondPassword) && firstPassword.matches("\\d{8}")) {
                newPassword = firstPassword;
                JOptionPane.showMessageDialog(this, "密码更改成功！");
                break;
            } else {
                JOptionPane.showMessageDialog(this, "密码不匹配或格式错误，请重新输入！");
            }
        }
        sendCommand("CHANGE" + newPassword);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(User1::new);
    }
}