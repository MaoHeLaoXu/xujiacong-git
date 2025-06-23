
// src/main/java/service/NormalStore.java
package service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import model.command.AbstractCommand;
import model.command.SetCommand;
import model.command.RmCommand;
import model.command.CommandPos;
import utils.CommandUtil;
import utils.LoggerUtil;
import utils.RandomAccessFileUtil;

import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 普通存储实现类
 */
public class NormalStore implements Store {
    /**
     * 内存表，有序结构
     */
    private TreeMap<String, Command> memTable;

    /**
     * hash索引，存储数据位置信息
     */
    private HashMap<String, CommandPos> index;

    /**
     * 数据目录
     */
    private final String dataDir;

    /**
     * 数据文件路径
     */
    private final String dataFilePath;

    /**
     * 读写锁，保证并发安全
     */
    private final ReadWriteLock indexLock;

    /**
     * 持久化阈值
     */
    private final int storeThreshold;

    /**
     * 日志记录器
     */
    private static final String LOGGER = "NormalStore";
    private static final String logFormat = "{} - {}";

    public NormalStore(String dataDir, int storeThreshold) {
        this.dataDir = dataDir;
        this.dataFilePath = dataDir + "/data.table";
        this.storeThreshold = storeThreshold;
        this.memTable = new TreeMap<>();
        this.index = new HashMap<>();
        this.indexLock = new ReentrantReadWriteLock();

        // 初始化数据目录
        RandomAccessFileUtil.initDir(dataDir);

        // 加载索引
        reloadIndex();
    }

    @Override
    public void set(String key, String value) {
        try {
            SetCommand command = new SetCommand(key, value);
            byte[] commandBytes = JSONObject.toJSONBytes(command);

            // 加写锁
            indexLock.writeLock().lock();

            // 写入WAL日志
            RandomAccessFileUtil.writeInt(dataFilePath, commandBytes.length);
            int pos = RandomAccessFileUtil.write(dataFilePath, commandBytes);

            // 更新索引
            CommandPos cmdPos = new CommandPos(pos, commandBytes.length);
            index.put(key, cmdPos);

            // 更新内存表
            memTable.put(key, command);

            // 检查是否需要刷盘
            checkAndFlush();

        } catch (Throwable t) {
            throw new RuntimeException("设置数据失败", t);
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    @Override
    public void rm(String key) {
        try {
            RmCommand command = new RmCommand(key);
            byte[] commandBytes = JSONObject.toJSONBytes(command);

            // 加写锁
            indexLock.writeLock().lock();

            // 写入WAL日志
            RandomAccessFileUtil.writeInt(dataFilePath, commandBytes.length);
            int pos = RandomAccessFileUtil.write(dataFilePath, commandBytes);

            // 更新索引
            CommandPos cmdPos = new CommandPos(pos, commandBytes.length);
            index.put(key, cmdPos);

            // 更新内存表
            memTable.put(key, command);

            // 检查是否需要刷盘
            checkAndFlush();

        } catch (Throwable t) {
            throw new RuntimeException("删除数据失败", t);
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    @Override
    public String get(String key) {
        try {
            // 加读锁
            indexLock.readLock().lock();

            // 从索引中获取位置信息
            CommandPos cmdPos = index.get(key);
            if (cmdPos == null) {
                return null;
            }

            // 从日志文件中读取命令
            byte[] commandBytes = RandomAccessFileUtil.readByIndex(
                    dataFilePath, cmdPos.getPos(), cmdPos.getLen());
            JSONObject jsonObject = JSONObject.parseObject(new String(commandBytes));
            AbstractCommand cmd = CommandUtil.jsonToCommand(jsonObject);

            // 根据命令类型返回结果
            if (cmd instanceof SetCommand) {
                return ((SetCommand) cmd).getValue();
            } else if (cmd instanceof RmCommand) {
                return null; // 已删除
            }

        } catch (Throwable t) {
            throw new RuntimeException("获取数据失败", t);
        } finally {
            indexLock.readLock().unlock();
        }
        return null;
    }

    @Override
    public void reloadIndex() {
        try {
            // 清空现有索引
            indexLock.writeLock().lock();
            index.clear();
            memTable.clear();

            RandomAccessFile file = new RandomAccessFile(dataFilePath, "rw");
            long len = file.length();
            long start = 0;
            file.seek(start);

            // 重做所有日志命令
            while (start < len) {
                int cmdLen = file.readInt();
                byte[] bytes = new byte[cmdLen];
                file.read(bytes);
                JSONObject jsonObject = JSON.parseObject(new String(bytes));
                AbstractCommand command = CommandUtil.jsonToCommand(jsonObject);

                start += 4; // 跳过长度标记
                if (command != null) {
                    CommandPos cmdPos = new CommandPos((int) start, cmdLen);
                    index.put(command.getKey(), cmdPos);
                    memTable.put(command.getKey(), (Command) command);
                }
                start += cmdLen;
            }
            file.seek(file.length()); // 将指针移到文件末尾

        } catch (Exception e) {
            throw new RuntimeException("重建索引失败", e);
        } finally {
            indexLock.writeLock().unlock();
            LoggerUtil.debug(LOGGER, logFormat, "索引重建完成，索引大小: " + index.size());
        }
    }

    /**
     * 检查内存表是否达到阈值并执行刷盘
     */
    private void checkAndFlush() {
        if (memTable.size() >= storeThreshold) {
            flushMemTableToDisk();
        }
    }

    /**
     * 将内存表数据刷盘
     */
    private void flushMemTableToDisk() {
        try {
            indexLock.writeLock().lock();
            // 这里可以实现将内存表数据刷盘的逻辑
            // 简化实现：直接清空内存表
            memTable.clear();
            LoggerUtil.debug(LOGGER, logFormat, "内存表刷盘完成，已清空");
        } catch (Exception e) {
            LoggerUtil.error(LOGGER, logFormat, "内存表刷盘失败", e);
        } finally {
            indexLock.writeLock().unlock();
        }
    }
}