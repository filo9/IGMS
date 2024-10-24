public class GatewayServerMain {
 public static void main(String[] args) throws Exception {
  System.out.println("网关，启动!");

  // 启动 Administrator
  Thread adminThread = new Thread(() -> {
   try {
    Administrator.main(null);
   } catch (Exception e) {
    e.printStackTrace();
   }
  });

  // 启动 GatewayServer
  Thread gatewayServerThread = new Thread(() -> {
   try {
    GatewayServer.main(null);
   } catch (Exception e) {
    e.printStackTrace();
   }
  });

  // 启动 GatewayServerii
  Thread gatewayServeriiThread = new Thread(() -> {
   try {
    GatewayServerii.main(null);
   } catch (Exception e) {
    e.printStackTrace();
   }
  });

  // 启动所有线程
  adminThread.start();
  gatewayServerThread.start();
  gatewayServeriiThread.start();

  // 等待所有线程执行完成（可选）
  adminThread.join();
  gatewayServerThread.join();
  gatewayServeriiThread.join();

  System.out.println("所有服务启动完成！");
 }
}
