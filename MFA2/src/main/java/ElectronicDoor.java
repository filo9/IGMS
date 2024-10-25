import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.TitledBorder;

public class ElectronicDoor {
 private JFrame frame;
 private JTextArea displayArea;
 private JPanel buttonPanel;
 private Point mousePoint; // 用于记录鼠标位置

 public ElectronicDoor() {
  createAndShowGUI();
 }

 private void createAndShowGUI() {
  frame = new JFrame("电子门控制系统");
  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  frame.setSize(400, 600);
  frame.setLayout(new BorderLayout());
  frame.setUndecorated(true); // 设为无边框

  // 设置窗口背景颜色为暗灰色
  frame.getContentPane().setBackground(new Color(50, 50, 50));

  // 鼠标拖动事件，放在按钮面板上
  buttonPanel = new JPanel();
  buttonPanel.setLayout(new GridLayout(4, 3)); // 4行3列的网格布局
  buttonPanel.setBackground(new Color(50, 50, 50)); // 设置按钮面板背景颜色
  buttonPanel.addMouseListener(new MouseAdapter() {
   @Override
   public void mousePressed(MouseEvent e) {
    mousePoint = e.getPoint(); // 记录鼠标按下的位置
   }
  });

  buttonPanel.addMouseMotionListener(new MouseAdapter() {
   @Override
   public void mouseDragged(MouseEvent e) {
    // 更新窗口位置
    Point newPoint = e.getLocationOnScreen();
    int x = newPoint.x - mousePoint.x;
    int y = newPoint.y - mousePoint.y;

    // 确保窗口不被拖出屏幕
    if (x < 0) x = 0;
    if (y < 0) y = 0;
    if (x + frame.getWidth() > Toolkit.getDefaultToolkit().getScreenSize().width) {
     x = Toolkit.getDefaultToolkit().getScreenSize().width - frame.getWidth();
    }
    if (y + frame.getHeight() > Toolkit.getDefaultToolkit().getScreenSize().height) {
     y = Toolkit.getDefaultToolkit().getScreenSize().height - frame.getHeight();
    }

    frame.setLocation(x, y);
   }
  });

  // 创建显示器区域
  displayArea = new JTextArea();
  displayArea.setEditable(false);
  displayArea.setFont(new Font("Monospaced", Font.PLAIN, 48)); // 设置字体大小为48
  displayArea.setBackground(new Color(70, 70, 70)); // 设置显示区域背景颜色
  displayArea.setForeground(new Color(0, 128, 0)); // 设置文本颜色为白色

  JScrollPane scrollPane = new JScrollPane(displayArea);
  scrollPane.setPreferredSize(new Dimension(400, 120)); // 设置显示器高度

  // 创建自定义边框，并设置背景色
  TitledBorder titledBorder = BorderFactory.createTitledBorder("电子门锁");
  titledBorder.setTitleColor(new Color(0, 128, 0)); // 设置标题文字颜色
  titledBorder.setTitleFont(new Font("Monospaced", Font.PLAIN, 20)); // 设置标题字体
  scrollPane.setBorder(titledBorder);

  // 自定义标题背景颜色
  JPanel titlePanel = new JPanel();
  titlePanel.setBackground(new Color(50, 50, 50)); // 设置标题区域背景颜色
  titlePanel.setBorder(titledBorder);
  scrollPane.setViewportBorder(BorderFactory.createEmptyBorder()); // 取消默认边框
  scrollPane.setViewportView(displayArea);
  frame.add(scrollPane, BorderLayout.NORTH); // 显示器占20%高度

  // 设置按钮面板的边距
  buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0)); // 上边距20像素

  // 添加按钮面板到框架
  addButtonsToPanel();
  frame.add(buttonPanel, BorderLayout.CENTER);

  frame.setVisible(true);
 }

 private void addButtonsToPanel() {
  // 创建数字按钮0-9
  for (int i = 0; i < 10; i++) {
   JButton button = new JButton(String.valueOf(i));
   button.setFont(new Font("Monospaced", Font.PLAIN, 50)); // 设置按钮字体大小为50
   button.setBackground(new Color(70, 70, 70)); // 设置按钮背景颜色
   button.setForeground(Color.WHITE); // 设置按钮文本颜色
   button.addActionListener(new ButtonClickListener());
   buttonPanel.add(button);
  }

  // 创建Enter和Back按钮
  JButton enterButton = new JButton("Enter");
  enterButton.setFont(new Font("Monospaced", Font.PLAIN, 30)); // 设置按钮字体大小为30
  enterButton.setBackground(new Color(70, 70, 70)); // 设置按钮背景颜色
  enterButton.setForeground(Color.WHITE); // 设置按钮文本颜色
  enterButton.addActionListener(new EnterButtonClickListener());
  buttonPanel.add(enterButton);

  JButton backButton = new JButton("←");
  backButton.setFont(new Font("Monospaced", Font.PLAIN, 60)); // 设置按钮字体大小为60
  backButton.setBackground(new Color(70, 70, 70)); // 设置按钮背景颜色
  backButton.setForeground(Color.WHITE); // 设置按钮文本颜色
  backButton.addActionListener(new BackButtonClickListener());
  buttonPanel.add(backButton);
 }

 // 按钮点击事件处理
 private class ButtonClickListener implements ActionListener {
  @Override
  public void actionPerformed(ActionEvent e) {
   JButton clickedButton = (JButton) e.getSource();
   String buttonText = clickedButton.getText();
   // 将点击的按钮文本添加到显示器
   displayArea.append(buttonText);
  }
 }

 // 处理 Enter 按钮点击事件
 private class EnterButtonClickListener implements ActionListener {
  @Override
  public void actionPerformed(ActionEvent e) {
   // 在显示器中换行
   displayArea.append("\n");
  }
 }

 // 处理 Back 按钮点击事件
 private class BackButtonClickListener implements ActionListener {
  @Override
  public void actionPerformed(ActionEvent e) {
   String currentText = displayArea.getText();
   if (currentText.length() > 0) {
    // 删除最后一个字符（退格）
    displayArea.setText(currentText.substring(0, currentText.length() - 1));
   }
  }
 }

 public static void main(String[] args) {
  SwingUtilities.invokeLater(ElectronicDoor::new);
 }
}
