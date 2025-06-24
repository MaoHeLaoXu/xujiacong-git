package controller;

import com.alibaba.fastjson.JSON;
import dto.ActionDTO;
import dto.RespDTO;
import model.command.AbstractCommand;
import model.command.RmCommand;
import model.command.SetCommand;
import service.Store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Socket服务器处理程序
 */
public class SocketServerHandler implements Runnable {
    private Socket clientSocket;
    private Store store;
    private AtomicBoolean running = new AtomicBoolean(true);

    public SocketServerHandler(Socket clientSocket, Store store) {
        this.clientSocket = clientSocket;
        this.store = store;
    }

    @Override
    public void run() {
        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream()) {

            byte[] buffer = new byte[1024];
            int length;

            while (running.get() && (length = inputStream.read(buffer)) != -1) {
                String request = new String(buffer, 0, length, StandardCharsets.UTF_8);
                System.out.println("收到客户端请求: " + request);

                // 处理请求
                String response = processRequest(request);

                // 发送响应
                outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }

        } catch (IOException e) {
            System.err.println("客户端处理异常: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.err.println("关闭客户端连接失败: " + e.getMessage());
            }
        }
    }

    private String processRequest(String request) {
        try {
            ActionDTO actionDTO = JSON.parseObject(request, ActionDTO.class);
            AbstractCommand command = actionDTO.getCommand();

            if (command == null) {
                return JSON.toJSONString(RespDTO.fail("无效的命令"));
            }

            switch (command.getType()) {
                case SET:
                    if (command instanceof SetCommand) {
                        SetCommand setCommand = (SetCommand) command;
                        store.set(setCommand.getKey(), setCommand.getValue());
                        return JSON.toJSONString(RespDTO.success("设置成功", null));
                    }
                    break;
                case RM:
                    if (command instanceof RmCommand) {
                        RmCommand rmCommand = (RmCommand) command;
                        store.rm(rmCommand.getKey());
                        return JSON.toJSONString(RespDTO.success("删除成功", null));
                    }
                    break;
                case GET:
                    if (command instanceof SetCommand) { // 简化处理，使用SetCommand存储get请求的key
                        SetCommand getCommand = (SetCommand) command;
                        String value = store.get(getCommand.getKey());
                        return JSON.toJSONString(RespDTO.success("获取成功", value));
                    }
                    break;
            }

            return JSON.toJSONString(RespDTO.fail("不支持的命令类型"));
        } catch (Exception e) {
            System.err.println("处理请求失败: " + e.getMessage());
            return JSON.toJSONString(RespDTO.fail("处理请求失败: " + e.getMessage()));
        }
    }
}
