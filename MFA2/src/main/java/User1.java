import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class User1 extends JFrame {
    private static String DEVICE_NAME = "卓爱同学";
    private static String TARGET_DEVICE_NAME = "ElectronicDoor";

    public User1() {
        setTitle("电子门控制系统");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 居中显示

        // 创建主面板
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1, 10, 10)); // 4行1列布局

        // 添加按钮
        JButton openButton = new JButton("开门");
        JButton closeButton = new JButton("关门");
        JButton changePasswordButton = new JButton("更改密码");
        JButton exitButton = new JButton("退出");

        // 为每个按钮添加事件监听器
        openButton.addActionListener(e -> sendCommand("OPEN"));
        closeButton.addActionListener(e -> sendCommand("CLOSE"));
        changePasswordButton.addActionListener(e -> changePassword());
        exitButton.addActionListener(e -> System.exit(0));

        // 将按钮添加到面板
        panel.add(openButton);
        panel.add(closeButton);
        panel.add(changePasswordButton);
        panel.add(exitButton);

        // 将面板添加到窗口
        add(panel);

        setVisible(true);
    }

    private void sendCommand(String command) {
        Client.setCommand(command);
        Client.setDeviceName(DEVICE_NAME);
        Client.setTargetDeviceName(TARGET_DEVICE_NAME);
        Client.main(null); // 启动TargetDevice
        JOptionPane.showMessageDialog(this, "命令已发送: " + command);
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
