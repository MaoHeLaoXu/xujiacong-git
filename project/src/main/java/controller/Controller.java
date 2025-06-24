package controller;
import java.io.IOException;
public interface Controller {
    void set(String key, String value);

    String get(String key);

    void rm(String key);

    void startServer();

    void close() throws IOException;
}
