// src/main/java/example/SocketClientUsage.java
package example;

import client.SocketClient;

/**
 * Socket客户端使用示例
 */
public class SocketClientUsage {
    public static void main(String[] args) {
        try {
            // 创建客户端
            SocketClient client = new SocketClient();

            // 存储数据
            client.set("name", "张三");
            client.set("age", "25");
            client.set("city", "北京");

            // 获取数据
            String name = client.get("name");
            System.out.println("姓名: " + name);

            String age = client.get("age");
            System.out.println("年龄: " + age);

            // 删除数据
            client.rm("age");
            age = client.get("age");
            System.out.println("删除后年龄: " + (age != null ? age : "已删除"));

            // 关闭客户端
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}