package controller;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.NormalStore;
import service.Store;
import utils.LoggerUtil;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@Setter
@Getter
public class SocketServerController implements Controller {

    private final Logger LOGGER = LoggerFactory.getLogger(SocketServerController.class);
    private final String logFormat = "[SocketServerController][{}]: {}";
    private String host;
    private int port;
    private Store store;

    private ServerSocket serverSocket;

    public SocketServerController(String host, int port, Store store) {
        this.host = host;
        this.port = port;
        this.store = store;
    }

    @Override
    public void set(String key, String value) {
        // 可根据需求实现
    }

    @Override
    public String get(String key) {
        // 可根据需求实现
        return null;
    }

    @Override
    public void rm(String key) {
        // 可根据需求实现
    }

    @Override
    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            LoggerUtil.info(LOGGER, logFormat, "startServer", "Server started, waiting for connections...");

            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    LoggerUtil.info(LOGGER, logFormat, "startServer", "New client connected");
                    // 为每个客户端连接创建一个新的线程
                    new Thread(new SocketServerHandler(socket, store)).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                LoggerUtil.info(LOGGER, logFormat, "stopServer", "Server stopped");
            }
            if (store != null) {
                store.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}