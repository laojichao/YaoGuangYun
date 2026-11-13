package com.yaoguangyun;

import com.yaoguangyun.example.NetworkExample;
import com.yaoguangyun.network.ServerConfig;
import com.yaoguangyun.network.TcpClient;
import com.yaoguangyun.test.NetworkTest;
import com.yaoguangyun.test.SimpleHttpServer;

import java.io.IOException;
import java.util.Locale;

/**
 * 主类 - 程序入口
 * 
 * YaoGuangYun 应用程序入口点
 * 启动程序并运行网络请求功能示例
 * 
 * 程序功能：
 * 1. 启动应用程序
 * 2. 初始化设备信息管理器
 * 3. 运行网络请求示例
 * 4. 展示迁移后的功能
 * 
 * 运行方式：
 * - Maven: mvn exec:java -Dexec.mainClass=com.yaoguangyun.Main [-Dexec.args="test"]
 * - IDE: 直接运行Main类
 * - 打包后: java -jar target/YaoGuangYun-1.0.0.jar [command]
 * 
 * 参数说明：
 * - 无参数：运行完整示例（默认本地，外部探测需 -Dyaoguangyun.external=true）
 * - test：运行功能测试（失败时以非0退出码结束）
 * - server [port]：启动本地测试服务器（默认端口取 server.http_port）
 * - help：显示帮助
 */
public class Main {
    
    /**
     * 程序主入口
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.out.println("瑶光云 应用启动");
        System.out.println("====================");

        int exitCode;
        try {
            exitCode = dispatch(args);
        } catch (Throwable t) {
            // 顶层兜底：不要把原始栈直接甩给用户，也不要以 0 退出
            System.err.println("程序执行失败: " + describe(t));
            exitCode = 1;
        } finally {
            // 进程收尾：释放共享 TCP 线程池
            TcpClient.shutdown();
        }

        if (exitCode == 0) {
            System.out.println("\n程序执行完成！");
        } else {
            System.out.println("\n程序执行结束，存在失败项（退出码 " + exitCode + "）");
            System.exit(exitCode);
        }
    }

    /**
     * 按命令分发，返回退出码。
     */
    private static int dispatch(String[] args) {
        if (args.length == 0) {
            // 默认运行完整示例：本地优先，失败数作为退出码
            return NetworkExample.run();
        }

        // 使用 Locale.ROOT，避免土耳其语等区域把 'I' 转成 'ı' 导致命令无法识别
        String command = args[0].toLowerCase(Locale.ROOT);

        switch (command) {
            case "test":
                if (args.length > 1) {
                    System.out.println("test 不接受额外参数");
                    showHelp();
                    return 2;
                }
                // 运行功能测试：把失败项数量作为退出码向上传递
                return NetworkTest.run();

            case "server":
                if (args.length > 2) {
                    System.out.println("server 最多接受一个端口参数");
                    showHelp();
                    return 2;
                }
                int port = ServerConfig.getHttpPort();
                if (args.length == 2) {
                    try {
                        port = Integer.parseInt(args[1].trim());
                    } catch (NumberFormatException e) {
                        System.out.println("非法端口: " + args[1]);
                        showHelp();
                        return 2;
                    }
                    if (port < 0 || port > 65535) {
                        System.out.println("端口必须在 0..65535 之间: " + port);
                        showHelp();
                        return 2;
                    }
                }
                return runServer(port);

            case "help":
                if (args.length > 1) {
                    System.out.println("help 不接受额外参数");
                    showHelp();
                    return 2;
                }
                showHelp();
                return 0;

            default:
                System.out.println("未知命令: " + command);
                showHelp();
                return 1;
        }
    }

    /**
     * 启动本地测试服务器并等待回车。
     */
    private static int runServer(int port) {
        SimpleHttpServer server = new SimpleHttpServer(port);
        boolean started = false;
        try {
            System.out.println("启动本地测试服务器...");
            server.start();
            started = true;
            System.out.println("服务器已启动，按Enter键停止...");
            System.in.read();
            return 0;
        } catch (IOException e) {
            System.err.println((started ? "服务器运行失败: " : "服务器启动失败: ") + describe(e));
            return 1;
        } finally {
            if (started) {
                server.stop();
            }
        }
    }

    private static String describe(Throwable e) {
        if (e == null) {
            return "null";
        }
        String message = e.getMessage();
        return (message == null || message.isEmpty()) ? e.getClass().getSimpleName() + "(无消息)" : message;
    }
    
    /**
     * 显示帮助信息
     */
    private static void showHelp() {
        System.out.println("\n使用方法:");
        System.out.println("  java -jar YaoGuangYun-1.0.0.jar [command]");
        System.out.println("\n可用命令:");
        System.out.println("  (无参数)       - 运行完整示例（默认本地；外部探测需 -Dyaoguangyun.external=true）");
        System.out.println("  test           - 运行功能测试（失败时返回非0退出码）");
        System.out.println("  server [port]  - 启动本地测试服务器（默认端口 " + ServerConfig.getHttpPort() + "）");
        System.out.println("  help           - 显示此帮助信息");
        System.out.println("\n示例:");
        System.out.println("  java -jar YaoGuangYun-1.0.0.jar");
        System.out.println("  java -jar YaoGuangYun-1.0.0.jar test");
        System.out.println("  java -jar YaoGuangYun-1.0.0.jar server 8080");
        System.out.println("  java -Dyaoguangyun.external=true -jar YaoGuangYun-1.0.0.jar");
    }
}
