package example;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.GZIPOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class KVClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner scanner;
    private Map<String, String> cache = new HashMap<>();
    private BufferedWriter fileWriter;
    private final int MAX_FILE_SIZE = 4 * 4; // 1 MB
    private final int MAX_FILES = 5; // 最多保留5个文件
    private List<String[]> serverList;

    public KVClient() {
        serverList = readServerListFromConfig();
    }

    private List<String[]> readServerListFromConfig() {
        List<String[]> servers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("servers.config"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 使用 : 作为分隔符
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    servers.add(parts);
                }
            }
        } catch (IOException e) {
            System.out.println("读取服务器配置文件时出错");
            e.printStackTrace();
        }
        return servers;
    }

    public void startConnection() {
        if (serverList.isEmpty()) {
            System.out.println("没有可用的服务器");
            return;
        }
        // 简单的轮询选择服务器
        for (String[] server : serverList) {
            String ip = server[0];
            int port = Integer.parseInt(server[1]);
            try {
                socket = new Socket(ip, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                scanner = new Scanner(System.in);
                System.out.println("已连接到服务器 " + ip + ":" + port);

                // 初始化文件写入器，将每次操作的数据写入到文件中
                fileWriter = new BufferedWriter(new FileWriter("操作总数.txt", true));
                break;
            } catch (IOException e) {
                System.out.println("无法连接到服务器 " + ip + ":" + port);
            }
        }
        if (socket == null) {
            System.out.println("所有服务器都无法连接");
            return;
        }

        try {
            while (true) {
                System.out.print("请输入命令(set <key> <value>, get <key>, rm <key>, batch <key1=value1 key2=value2 ...>, exit): ");
                String userInput = scanner.nextLine();
                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                } else if (userInput.startsWith("batch ")) {
                    handleBatch(userInput.substring(6));
                } else {
                    out.println(userInput);
                    String response = in.readLine();
                    System.out.println("服务器响应： " + response);
                    // 将操作写入文件
                    writeToFile(userInput);
                }
            }
        } catch (IOException e) {
            System.out.println("通信错误");
            e.printStackTrace();
        } finally {
            stopConnection();
        }
    }

    private void writeToFile(String operation) {
        try {
            fileWriter.write(operation + "\n");
            fileWriter.flush(); // 确保写入文件
            rotateAndCompressIfNeeded();
        } catch (IOException e) {
            System.out.println("写入文件时出错");
            e.printStackTrace();
        }
    }

    private void rotateAndCompressIfNeeded() {
        File currentFile = new File("操作总数.txt");
        long fileSize = currentFile.length();
        if (fileSize > MAX_FILE_SIZE) {
            rotateFiles();
            compressFile(currentFile);
        }
    }

    private void rotateFiles() {
        File currentFile = new File("操作总数.txt");
        for (int i = MAX_FILES - 2; i >= 0; i--) {
            File nextFile = new File("操作总数_" + i + ".txt");
            if (nextFile.exists()) {
                nextFile.renameTo(new File("操作总数_" + (i + 1) + ".txt"));
            }
        }
        currentFile.renameTo(new File("操作总数_0.txt"));
    }

    private void compressFile(File fileToCompress) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try (FileInputStream fis = new FileInputStream(fileToCompress);
                 FileOutputStream fos = new FileOutputStream("操作总数.txt.gz");
                 GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {

                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    gzipOS.write(buffer, 0, len);
                }
                gzipOS.finish();
                System.out.println("压缩完成：" + fileToCompress.getName() + " -> 操作总数.txt.gz");

            } catch (IOException e) {
                System.out.println("压缩文件时出错");
                e.printStackTrace();
            }
        });
        executor.shutdown();
    }

    private void handleBatch(String keyValuePairs) {
        String[] pairs = keyValuePairs.split("\\s+");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                cache.put(kv[0], kv[1]);
                out.println("set " + kv[0] + " " + kv[1]);
                writeToFile("set " + kv[0] + " " + kv[1]);
            }
        }
        try {
            // 等待服务器响应
            for (int i = 0; i < pairs.length; i++) {
                System.out.println("服务器响应： " + in.readLine());
            }
        } catch (IOException e) {
            System.out.println("处理批量写入时通信错误");
            e.printStackTrace();
        }
    }

    public void stopConnection() {
        try {
            in.close();
            out.close();
            scanner.close();
            socket.close();
            fileWriter.close(); // 关闭文件写入器
        } catch (IOException e) {
            System.out.println("关闭连接时出错");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        KVClient client = new KVClient();
        client.startConnection();
    }
}