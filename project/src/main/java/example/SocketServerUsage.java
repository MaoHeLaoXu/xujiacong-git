package example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

class KVServer {
    private Map<String, String> keyValueStore;
    private String logPath;

    public KVServer() {
        keyValueStore = new HashMap<>();
        logPath = "D:/Desktop/xjc-java/数据库更新日志/数据库更新日志";
        initLogFile();
    }

    private void initLogFile() {
        try {
            Files.createDirectories(Paths.get(logPath));
        } catch (IOException e) {
            System.out.println("无法创建日志文件夹");
            e.printStackTrace();
        }
    }

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("服务器已启动，正在监听端口 " + port + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端连接成功");
                // 为每个客户端连接创建一个新的线程
                new ClientHandler(clientSocket, keyValueStore, logPath).start();
            }
        } catch (IOException e) {
            System.out.println("服务器异常");
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket clientSocket;
        private final Map<String, String> keyValueStore;
        private final String logPath;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket, Map<String, String> keyValueStore, String logPath) {
            this.clientSocket = socket;
            this.keyValueStore = keyValueStore;
            this.logPath = logPath;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String userInput;
                while ((userInput = in.readLine()) != null) {
                    System.out.println("客户端请求: " + userInput);
                    String[] commandParts = userInput.split("\\s+", 3);
                    String command = commandParts[0];

                    if ("set".equalsIgnoreCase(command)) {
                        String key = commandParts[1];
                        String value = commandParts[2];
                        addToLog("set", key, value);
                        keyValueStore.put(key, value);
                        out.println("Key " + key + " 设置成功");
                    } else if ("get".equalsIgnoreCase(command)) {
                        String key = commandParts[1];
                        String value = keyValueStore.get(key);
                        if (value != null) {
                            out.println(value);
                        } else {
                            out.println("无法找到该key");
                        }
                    } else if ("rm".equalsIgnoreCase(command)) {
                        String key = commandParts[1];
                        String removedValue = keyValueStore.remove(key);
                        if (removedValue != null) {
                            addToLog("rm", key, removedValue);
                            out.println("Key " + key + " 成功删除");
                        } else {
                            out.println("无法找到该key");
                        }
                    } else {
                        out.println("无效命令");
                    }
                }
            } catch (IOException e) {
                System.out.println("客户端连接异常");
                e.printStackTrace();
            } finally {
                stopHandler();
            }
        }

        private void addToLog(String operation, String key, String value) {
            LocalDateTime timestamp = LocalDateTime.now();
            String logFileName = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")) + ".log";
            Path logFilePath = Paths.get(logPath + logFileName);

            try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFilePath.toString(), true))) {
                logWriter.println(operation + " " + key + " " + value);
            } catch (IOException e) {
                System.out.println("无法写入日志文件");
                e.printStackTrace();
            }
        }

        private void stopHandler() {
            try {
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("关闭连接时出错");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        KVServer server = new KVServer();
        server.start(1234);
    }
}