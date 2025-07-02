package example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

class NoSQLServer1 {
    private static final int PORT = 12346;
    private static final String DATA_FILE_PATH = "data.txt";
    private static final String LOG_FILE_PATH = "server_log.log";
    private static final long MAX_LOG_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static boolean IS_MASTER;

    private static Map<String, String> dataCache = new ConcurrentHashMap<>();
    private static Map<String, Long> index = new ConcurrentHashMap<>();
    private static LogWriter logWriter;

    public static void main(String[] args) {
        loadConfig();
        loadDataAndBuildIndex();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logWriter = new LogWriter(LOG_FILE_PATH, MAX_LOG_FILE_SIZE);
            System.out.println("NoSQL服务器正在端口上运行 " + PORT + (IS_MASTER ? " (主节点)" : " (从节点)"));
            logWriter.write("NoSQL服务器在端口上启动 " + PORT + (IS_MASTER ? " (主节点)" : " (从节点)"));

            ExecutorService executor = Executors.newFixedThreadPool(10);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadConfig() {
        try (InputStream input = new FileInputStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            String nodeName = "node" + System.getProperty("node.id", "2");
            IS_MASTER = "master".equalsIgnoreCase(prop.getProperty(nodeName + ".role"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static String processRequest(String request) {
        String[] parts = request.split(" ", 2);
        if (parts.length < 2) return "无效请求";

        String command = parts[0].toUpperCase();
        String payload = parts[1];

        if (!IS_MASTER && (command.equals("SET") || command.equals("RM"))) {
            return "从节点不支持写操作";
        }

        switch (command) {
            case "GET":
                // 先从缓存查找
                if (dataCache.containsKey(payload)) {
                    return dataCache.get(payload);
                }
                // 再从索引查找
                if (index.containsKey(payload)) {
                    String value = readValueFromFile(payload);
                    dataCache.put(payload, value);
                    return value;
                }
                return "key不存在";
            case "SET":
                String[] kv = payload.split(" ", 2);
                if (kv.length < 2) return "set请求无效";
                dataCache.put(kv[0], kv[1]);
                saveDataAndUpdateIndex(kv[0], kv[1]);
                return "OK";
            case "RM":
                dataCache.remove(payload);
                index.remove(payload);
                saveData();
                return "OK";
            default:
                return "不知名的命令";
        }
    }

    private static void loadDataAndBuildIndex() {
        try (RandomAccessFile raf = new RandomAccessFile(DATA_FILE_PATH, "r")) {
            long offset = 0;
            String line;
            while ((line = raf.readLine()) != null) {
                String[] kv = line.split(" ", 2);
                if (kv.length == 2) {
                    index.put(kv[0], offset);
                    dataCache.put(kv[0], kv[1]); // 加载数据到缓存
                }
                offset = raf.getFilePointer();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String readValueFromFile(String key) {
        try (RandomAccessFile raf = new RandomAccessFile(DATA_FILE_PATH, "r")) {
            long offset = index.get(key);
            raf.seek(offset);
            String line = raf.readLine();
            String[] kv = line.split(" ", 2);
            if (kv.length == 2) {
                return kv[1];
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "key不存在";
    }

    private static void saveDataAndUpdateIndex(String key, String value) {
        try (RandomAccessFile raf = new RandomAccessFile(DATA_FILE_PATH, "rw")) {
            if (index.containsKey(key)) {
                // 覆盖原有记录
                long offset = index.get(key);
                raf.seek(offset);
                raf.writeBytes(key + " " + value + "\n");
            } else {
                // 追加新记录
                raf.seek(raf.length());
                long newOffset = raf.getFilePointer();
                raf.writeBytes(key + " " + value + "\n");
                index.put(key, newOffset);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveData() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE_PATH))) {
            for (Map.Entry<String, String> entry : dataCache.entrySet()) {
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

            // 异步压缩文件
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

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                String request;
                while ((request = in.readLine()) != null) {
                    String response = processRequest(request);
                    out.println(response);
                    logWriter.write("收到: " + request + " | 回答: " + response);
                }
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    logWriter.write("错误: " + e.getMessage());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}