package controller;

import service.Store;
import service.NormalStore;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Socket服务器控制器
 */
public class SocketServerController {
    private static final int PORT = 8888;
    private static final String DATA_DIR = "data";
    private static final int STORE_THRESHOLD = 1000;

    private Store store;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private boolean running = false;

    public SocketServerController() {
        this.store = new NormalStore(DATA_DIR, STORE_THRESHOLD);
        this.executorService = Executors.newFixedThreadPool(10);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("服务器启动成功，监听端口: " + PORT);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("新客户端连接: " + clientSocket.getInetAddress());

                // 为每个客户端创建一个处理线程
                SocketServerHandler handler = new SocketServerHandler(clientSocket, store);
                executorService.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            executorService.shutdown();
            System.out.println("服务器已关闭");
        } catch (IOException e) {
            System.err.println("关闭服务器失败: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SocketServerController server = new SocketServerController();
        server.start();
    }
}