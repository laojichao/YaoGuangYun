package com.yaoguangyun;

import com.yaoguangyun.example.NetworkExample;
import com.yaoguangyun.test.NetworkTest;

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
 * - Windows: run.bat
 * - Linux/Mac: ./run.sh
 * - IDE: 直接运行Main类
 * 
 * 参数说明：
 * - 无参数：运行完整示例
 * - test：运行功能测试
 * - server：启动本地测试服务器
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
        
        // 检查命令行参数
        if (args.length > 0) {
            String command = args[0].toLowerCase();
            
            switch (command) {
                case "test":
                    // 运行功能测试
                    NetworkTest.main(args);
                    break;
                    
                case "server":
                    // 启动本地测试服务器
                    System.out.println("启动本地测试服务器...");
                    try {
                        com.yaoguangyun.test.SimpleHttpServer server = new com.yaoguangyun.test.SimpleHttpServer(8080);
                        server.start();
                        System.out.println("服务器已启动，按Enter键停止...");
                        System.in.read();
                        server.stop();
                    } catch (Exception e) {
                        System.err.println("服务器启动失败: " + e.getMessage());
                    }
                    break;
                    
                case "help":
                    // 显示帮助信息
                    showHelp();
                    break;
                    
                default:
                    System.out.println("未知命令: " + command);
                    showHelp();
                    break;
            }
        } else {
            // 默认运行完整示例
            NetworkExample.main(args);
        }
        
        System.out.println("\n程序执行完成！");
    }
    
    /**
     * 显示帮助信息
     */
    private static void showHelp() {
        System.out.println("\n使用方法:");
        System.out.println("  java -cp ... com.yaoguangyun.Main [command]");
        System.out.println("\n可用命令:");
        System.out.println("  (无参数)  - 运行完整示例");
        System.out.println("  test      - 运行功能测试");
        System.out.println("  server    - 启动本地测试服务器");
        System.out.println("  help      - 显示此帮助信息");
        System.out.println("\n示例:");
        System.out.println("  java -cp ... com.yaoguangyun.Main");
        System.out.println("  java -cp ... com.yaoguangyun.Main test");
        System.out.println("  java -cp ... com.yaoguangyun.Main server");
    }
}