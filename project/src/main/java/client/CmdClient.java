package client;

import java.util.Scanner;

public class CmdClient {
    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static final Client client = new SocketClient(HOST, PORT);

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter command (set <key> <value>, get <key>, rm <key>, exit): ");
            String input = scanner.nextLine();
            String[] parts = input.split(" ");
            String command = parts[0];

            switch (command) {
                case "set":
                    if (parts.length == 3) {
                        String key = parts[1];
                        String value = parts[2];
                        client.set(key, value);
                    } else {
                        System.out.println("Invalid set command. Usage: set <key> <value>");
                    }
                    break;
                case "get":
                    if (parts.length == 2) {
                        String key = parts[1];
                        String result = client.get(key);
                        System.out.println("Value: " + result);
                    } else {
                        System.out.println("Invalid get command. Usage: get <key>");
                    }
                    break;
                case "rm":
                    if (parts.length == 2) {
                        String key = parts[1];
                        client.rm(key);
                    } else {
                        System.out.println("Invalid rm command. Usage: rm <key>");
                    }
                    break;
                case "exit":
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid command. Please try again.");
            }
        }
    }
}