import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;

public class RunPythonScript {

    public static void main(String[] args) {
        try {
            // 设置要执行Python脚本的目录路径
            File projectDir = new File("/Users/filo/Documents/GitHub/MFA2/MFA2");

            // 创建ProcessBuilder对象，指定Python命令和脚本路径
            ProcessBuilder processBuilder = new ProcessBuilder("/Users/filo/.pyenv/shims/python", "script.py");

            // 设置工作目录
            processBuilder.directory(projectDir);

            // 启动进程
            Process process = processBuilder.start();

            // 读取并输出脚本的标准输出
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = stdInput.readLine()) != null) {
                System.out.println(line);
            }

            // 读取并输出脚本的错误输出（例如，ModuleNotFound错误）
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = stdError.readLine()) != null) {
                System.out.println(line);
            }

            // 等待脚本执行结束并获取退出码
            //int exitCode = process.waitFor();
            //System.out.println("Exited with code: " + exitCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}