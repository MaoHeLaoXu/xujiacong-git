package example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class NoSQLServer {
    private static final int PORT = 12345;
    private static final String DATA_FILE_PATH = "data.txt";
    private static final String LOG_FILE_PATH = "server_log.log";
    private static final long MAX_LOG_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private static Map<String, String> dataStore = new ConcurrentHashMap<>();
    private static LogWriter logWriter;

    public static void main(String[] args) {
        loadData();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logWriter = new LogWriter(LOG_FILE_PATH, MAX_LOG_FILE_SIZE);
            System.out.println("NoSQL服务器正在端口上运行 " + PORT);
            logWriter.write("NoSQL服务器在端口上启动 " + PORT);

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                    String request;
                    while ((request = in.readLine()) != null) {
                        String response = processRequest(request);
                        out.println(response);
                        logWriter.write("收到: " + request + " | 回答: " + response);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    logWriter.write("错误: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String processRequest(String request) {
        String[] parts = request.split(" ", 2);
        if (parts.length < 2) return "无效请求";

        String command = parts[0].toUpperCase();
        String payload = parts[1];

        switch (command) {
            case "GET":
                return dataStore.getOrDefault(payload, "key不存在");
            case "SET":
                String[] kv = payload.split(" ", 2);
                if (kv.length < 2) return "set请求无效";
                dataStore.put(kv[0], kv[1]);
                saveData();
                return "OK";
            case "RM":
                dataStore.remove(payload);
                saveData();
                return "OK";
            default:
                return "不知名的命令";
        }
    }

    private static void loadData() {
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] kv = line.split(" ", 2);
                if (kv.length == 2) {
                    dataStore.put(kv[0], kv[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveData() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE_PATH))) {
            for (Map.Entry<String, String> entry : dataStore.entrySet()) {
                writer.println(entry.getKey() + " " + entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class LogWriter {
        private final String logFilePath;
        private final long maxFileSize;
        private long currentFileSize;
        private PrintWriter writer;
        private final ExecutorService executor;

        public LogWriter(String logFilePath, long maxFileSize) throws IOException {
            this.logFilePath = logFilePath;
            this.maxFileSize = maxFileSize;
            this.writer = new PrintWriter(new BufferedWriter(new FileWriter(logFilePath, true)));
            this.currentFileSize = new File(logFilePath).length();
            this.executor = Executors.newFixedThreadPool(2);
        }

        public synchronized void write(String message) throws IOException {
            String logEntry = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " " + message;
            writer.println(logEntry);
            writer.flush();
            currentFileSize += logEntry.length();

            if (currentFileSize >= maxFileSize) {
                rotateLogFile();
            }
        }

        private void rotateLogFile() throws IOException {
            writer.close();
            String rotatedFileName = logFilePath + "." + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            Files.move(Paths.get(logFilePath), Paths.get(rotatedFileName));
            this.writer = new PrintWriter(new BufferedWriter(new FileWriter(logFilePath, true)));
            this.currentFileSize = 0;

            // Compress the rotated file
            executor.submit(() -> compressFile(rotatedFileName));
        }

        private void compressFile(String fileName) {
            try {
                String zipFileName = fileName + ".zip";
                try (FileInputStream fis = new FileInputStream(fileName);
                     FileOutputStream fos = new FileOutputStream(zipFileName);
                     BufferedInputStream bis = new BufferedInputStream(fis);
                     ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {

                    zos.putNextEntry(new ZipEntry(new File(fileName).getName()));

                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = bis.read(buffer)) != -1) {
                        zos.write(buffer, 0, read);
                    }

                    zos.closeEntry();
                }
                Files.delete(Paths.get(fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void shutdown() {
            executor.shutdown();
            writer.close();
        }
    }
}
