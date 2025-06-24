package example;

import java.io.*;  // 基本的输入输出类，如InputStream、OutputStream、Reader、Writer
import java.net.ServerSocket;  // 提供监听传入网络请求的机制（服务器端）
import java.net.Socket;  // 表示网络通信链路中的客户端或服务器端的端点
import java.nio.file.Files;  // 使用NIO（New I/O）API支持文件输入输出操作
import java.nio.file.Path;  // 表示文件或目录路径
import java.nio.file.Paths;  // 提供操作文件和目录路径的方法
import java.time.LocalDateTime;  // 表示不带时区信息的日期和时间
import java.time.format.DateTimeFormatter;  // 提供格式化和解析日期时间对象的方法
import java.util.HashMap;  // 实现了Map接口，使用哈希表存储键值对
import java.util.Map;  // 提供了存储和访问基于键值对的数据的方式

class KVServer {
    private Map<String, String> keyValueStore;//存储键值对数据
    private PrintWriter out;//向输出流写入文本数据
    private BufferedReader in;//用于从输入流中读取文本数据
    private String logPath;//存储日志文件的路径

    public KVServer() {
        keyValueStore = new HashMap<>();// 初始化一个HashMap实例作为键值对存储容器
        logPath = "D:/Desktop/javakvsql/数据库更新日志/数据库更新日志"; // 定义日志文件存储路径
        initLogFile();// 调用初始化日志文件的方法
    }

    private void initLogFile() {
        try {
            // 创建日志文件夹
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
                Socket clientSocket = serverSocket.accept();// 创建了一个服务器套接字，监听指定的端口 port
                System.out.println("客户端连接成功");

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
                        addToLog("set", key, value); // 添加到日志
                        keyValueStore.put(key, value);
                        out.println("Key " + key + " 设置成功");
                    } else if ("get".equalsIgnoreCase(command)) {//忽略大小写。如果命令是 "get"，则执行接下来的代码块。
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
                            addToLog("rm", key, removedValue); // 添加到日志
                            out.println("Key " + key + " 成功删除");
                        } else {
                            out.println("无法找到该key");
                        }
                    } else {
                        out.println("无效命令");
                    }
                }
            }
        } catch (IOException e) {//指定了异常处理块，捕获可能抛出的 IOException 异常及其子类
            System.out.println("服务器异常");
            e.printStackTrace();//调用异常对象 e 的 printStackTrace() 方法，将异常堆栈信息打印到控制台
        } finally {
            stop();
        }
    }

    private void addToLog(String operation, String key, String value) {//将操作日志写入到文件中
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

    public void stop() {//关闭输入输出流
        try {
            in.close();
            out.close();
        } catch (IOException e) {
            System.out.println("关闭连接时出错");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        KVServer server = new KVServer();//创建KVserver实例
        server.start(1234);//启动服务器
    }
}
