package client;

import dto.ActionDTO;
import dto.ActionTypeEnum;
import dto.RespDTO;
import java.io.*;
import java.net.Socket;

public class SocketClient implements Client {
    private String host;
    private int port;

    public SocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void set(String key, String value) {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            // 传输序列化对象
            ActionDTO dto = new ActionDTO(ActionTypeEnum.SET, key, value);
            oos.writeObject(dto);
            oos.flush();
            RespDTO resp = (RespDTO) ois.readObject();
            System.out.println("resp data: " + resp.toString());
            // 接收响应数据
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String get(String key) {
        String result = null;
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            // 传输序列化对象
            ActionDTO dto = new ActionDTO(ActionTypeEnum.GET, key, null);
            oos.writeObject(dto);
            oos.flush();
            RespDTO resp = (RespDTO) ois.readObject();
            System.out.println("resp data: " + resp.toString());
            result = resp.getValue();
            // 接收响应数据
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void rm(String key) {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            // 传输序列化对象
            ActionDTO dto = new ActionDTO(ActionTypeEnum.REMOVE, key, null);
            oos.writeObject(dto);
            oos.flush();
            RespDTO resp = (RespDTO) ois.readObject();
            System.out.println("resp data: " + resp.toString());
            // 接收响应数据
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}