package example;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.*;

class NoSQLClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
             LogWriter logWriter = new LogWriter()) {

            System.out.println("已连接到位于这里的NoSQL服务器 " + SERVER_ADDRESS + ":" + SERVER_PORT);
            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput);
                String serverResponse = in.readLine();
                System.out.println("服务器响应: " + serverResponse);
                logWriter.write("Client: " + userInput);
                logWriter.write("Server: " + serverResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class LogWriter implements AutoCloseable {
        private static final String LOG_FILE_PATH = "client_log.log";
        private static final long MAX_FILE_SIZE =  1024 * 1024; // 10 MB
        private long currentFileSize;
        private PrintWriter writer;

        public LogWriter() throws IOException {
            this.writer = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE_PATH, true)));
            this.currentFileSize = new File(LOG_FILE_PATH).length();
        }

        public synchronized void write(String message) throws IOException {
            String logEntry = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " " + message;
            writer.println(logEntry);
            writer.flush();
            currentFileSize += logEntry.length();

            if (currentFileSize >= MAX_FILE_SIZE) {
                rotateLogFile();
            }
        }

        private void rotateLogFile() throws IOException {
            writer.close();
            String rotatedFileName = LOG_FILE_PATH + "." + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            Files.move(Paths.get(LOG_FILE_PATH), Paths.get(rotatedFileName));
            this.writer = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE_PATH, true)));
            this.currentFileSize = 0;

            // Compress the rotated file
            compressFile(rotatedFileName);
        }

        private void compressFile(String fileName) throws IOException {
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
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }
    }
}
