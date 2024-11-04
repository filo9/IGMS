import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;

public class RunPythonScript {

    public static void main(String[] args) {

        try {

            // 设置Python脚本的目录路径
            File projectDir;
            String pythonPath;
            String scriptName = "rev.py"; // Python脚本名称
            // 根据操作系统选择路径
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                projectDir = new File("c:/Users/17285/Desktop/MFA2/MFA2/src/main/python");
                pythonPath = "C:/Users/17285/AppData/Local/Programs/Python/Python312/python.exe";
            } else { // 假设是macOS或Linux
                projectDir = new File("/Users/filo/Documents/GitHub/MFA2/MFA2/src/main/python");
                pythonPath = "/Users/filo/.pyenv/shims/python"; // 或者你的Python路径
            }

            // 创建ProcessBuilder对象，指定Python路径和脚本名
            ProcessBuilder processBuilder = new ProcessBuilder(pythonPath, scriptName);

            // 设置工作目录
            processBuilder.directory(projectDir);

            // 启动进程
            Process process = processBuilder.start();

            // 创建一个输出流，用于向Python脚本发送输入
            OutputStream os = process.getOutputStream();
            PrintWriter writer = new PrintWriter(os);

            // 向Python脚本发送输入，比如选择“1”或“2”
            writer.println("2"); // 输入1或2，选择录音或从文件读取
            writer.flush(); // 刷新输出流，确保数据发送到Python脚本

            // 读取并输出脚本的标准输出
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = stdInput.readLine()) != null) {
                System.out.println(line);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
