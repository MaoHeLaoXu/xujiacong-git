// src/main/java/client/CmdClient.java
package client;

import java.util.Scanner;

/**
 * 命令行客户端
 */
public class CmdClient {
    private SocketClient socketClient;
    private Scanner scanner;

    public CmdClient() {
        socketClient = new SocketClient();
        scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("===== KV数据库命令行客户端 =====");
        System.out.println("支持命令: set [key] [value], get [key], rm [key], exit");

        boolean running = true;
        while (running) {
            System.out.print("> ");
            String command = scanner.nextLine().trim();

            if (command.equalsIgnoreCase("exit")) {
                running = false;
            } else if (command.startsWith("set ")) {
                String[] parts = command.split(" ", 3);
                if (parts.length == 3) {
                    String key = parts[1];
                    String value = parts[2];
                    socketClient.set(key, value);
                } else {
                    System.out.println("语法错误: set [key] [value]");
                }
            } else if (command.startsWith("get ")) {
                String[] parts = command.split(" ", 2);
                if (parts.length == 2) {
                    String key = parts[1];
                    String value = socketClient.get(key);
                    if (value != null) {
                        System.out.println("值: " + value);
                    } else {
                        System.out.println("未找到该键或已被删除");
                    }
                } else {
                    System.out.println("语法错误: get [key]");
                }
            } else if (command.startsWith("rm ")) {
                String[] parts = command.split(" ", 2);
                if (parts.length == 2) {
                    String key = parts[1];
                    socketClient.rm(key);
                } else {
                    System.out.println("语法错误: rm [key]");
                }
            } else {
                System.out.println("未知命令，请输入 help 查看帮助");
            }
        }

        // 关闭客户端
        socketClient.close();
        scanner.close();
        System.out.println("客户端已退出");
    }

    public static void main(String[] args) {
        CmdClient cmdClient = new CmdClient();
        cmdClient.start();
    }
}