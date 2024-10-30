import javax.swing.*;
import java.awt.*;

public class USER extends JFrame {

 public USER() {
  setTitle("选择程序启动");
  setSize(300, 200);
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  setLocationRelativeTo(null); // 窗口居中显示

  // 创建面板并设置布局
  JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));

  // 创建按钮用于选择启动不同的类
  JButton buttonUser1 = new JButton("控制门");
  JButton buttonUser2 = new JButton("控制灯");
  JButton buttonUser3 = new JButton("控制空调");
  JButton exitButton = new JButton("退出");

  // 绑定按钮事件，分别启动不同的功能窗口
  buttonUser1.addActionListener(e -> openUserWindow(new User1(), "门控制"));
  buttonUser2.addActionListener(e -> openUserWindow(new User2(), "灯光控制"));
  buttonUser3.addActionListener(e -> openUserWindow(new User3(), "空调控制"));
  exitButton.addActionListener(e -> System.exit(0)); // 退出程序

  // 将按钮添加到面板
  panel.add(buttonUser1);
  panel.add(buttonUser2);
  panel.add(buttonUser3);
  panel.add(exitButton);

  // 将面板添加到窗口
  add(panel);
  setVisible(true);
 }

 // 打开新窗口的方法
 private void openUserWindow(JFrame window, String title) {
  window.setTitle(title);
  window.setSize(400, 300);
  window.setLocationRelativeTo(null); // 新窗口居中显示
  window.setVisible(true);
 }

 // 主方法，启动选择界面
 public static void main(String[] args) {
  SwingUtilities.invokeLater(USER::new);
 }
}
