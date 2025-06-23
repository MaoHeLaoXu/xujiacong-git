// src/main/java/utils/RandomAccessFileUtil.java
package utils;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/**
 * 随机访问文件工具类
 */
public class RandomAccessFileUtil {
    private static final String RW_MODE = "rw";

    /**
     * 初始化数据目录
     */
    public static void initDir(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * 写入整数到文件
     */
    public static void writeInt(String filePath, int value) throws Exception {
        RandomAccessFile file = new RandomAccessFile(filePath, RW_MODE);
        file.seek(file.length());
        file.writeInt(value);
        file.close();
    }

    /**
     * 写入字节数组到文件
     */
    public static int write(String filePath, byte[] bytes) throws Exception {
        RandomAccessFile file = new RandomAccessFile(filePath, RW_MODE);
        long pos = file.length();
        file.seek(pos);
        file.write(bytes);
        int len = bytes.length;
        file.close();
        return (int) pos;
    }

    /**
     * 根据索引读取文件内容
     */
    public static byte[] readByIndex(String filePath, int pos, int len) throws Exception {
        RandomAccessFile file = new RandomAccessFile(filePath, "r");
        file.seek(pos);
        byte[] bytes = new byte[len];
        file.readFully(bytes);
        file.close();
        return bytes;
    }
}
