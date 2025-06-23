// src/main/java/service/Store.java
package service;

/**
 * 存储服务接口
 */
public interface Store {
    void set(String key, String value);
    void rm(String key);
    String get(String key);
    void reloadIndex();
}
