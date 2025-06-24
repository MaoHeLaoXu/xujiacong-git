package client;
import com.alibaba.fastjson.JSON;
import dto.ActionDTO;
import dto.RespDTO;
import model.command.SetCommand;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Socket客户端
 */
public class SocketClient {
    private static final String HOST = "localhost";
    private static final int PORT = 8888;
    private Socket socket;

    public SocketClient() {
        try {
            socket = new Socket(HOST, PORT);
            System.out.println("连接服务器成功");
        } catch (IOException e) {
            throw new RuntimeException("连接服务器失败", e);
        }
    }

    public void set(String key, String value) {
        SetCommand command = new SetCommand(key, value);
        ActionDTO actionDTO = new ActionDTO(command);
        String request = JSON.toJSONString(actionDTO);

        try (OutputStream outputStream = socket.getOutputStream();
             InputStream inputStream = socket.getInputStream()) {

            outputStream.write(request.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            byte[] buffer = new byte[1024];
            int length = inputStream.read(buffer);
            String response = new String(buffer, 0, length, StandardCharsets.UTF_8);

            RespDTO respDTO = JSON.parseObject(response, RespDTO.class);
            System.out.println("设置结果: " + respDTO.getMessage());

        } catch (IOException e) {
            throw new RuntimeException("设置数据失败", e);
        }
    }

    public String get(String key) {
        SetCommand command = new SetCommand(key, ""); // 简化处理，value无用
        ActionDTO actionDTO = new ActionDTO(command);
        String request = JSON.toJSONString(actionDTO);

        try (OutputStream outputStream = socket.getOutputStream();
             InputStream inputStream = socket.getInputStream()) {

            outputStream.write(request.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            byte[] buffer = new byte[1024];
            int length = inputStream.read(buffer);
            String response = new String(buffer, 0, length, StandardCharsets.UTF_8);

            RespDTO respDTO = JSON.parseObject(response, RespDTO.class);
            if (respDTO.getStatus() == RespDTO.RespStatusTypeEnum.SUCCESS) {
                return (String) respDTO.getData();
            } else {
                System.out.println("获取失败: " + respDTO.getMessage());
                return null;
            }

        } catch (IOException e) {
            throw new RuntimeException("获取数据失败", e);
        }
    }

    public void rm(String key) {
        RmCommand command = new RmCommand(key);
        ActionDTO actionDTO = new ActionDTO(command);
        String request = JSON.toJSONString(actionDTO);

        try (OutputStream outputStream = socket.getOutputStream();
             InputStream inputStream = socket.getInputStream()) {

            outputStream.write(request.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            byte[] buffer = new byte[1024];
            int length = inputStream.read(buffer);
            String response = new String(buffer, 0, length, StandardCharsets.UTF_8);

            RespDTO respDTO = JSON.parseObject(response, RespDTO.class);
            System.out.println("删除结果: " + respDTO.getMessage());

        } catch (IOException e) {
            throw new RuntimeException("删除数据失败", e);
        }
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("客户端已关闭");
        } catch (IOException e) {
            System.err.println("关闭客户端失败: " + e.getMessage());
        }
    }
}