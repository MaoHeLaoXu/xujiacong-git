// src/main/java/utils/LoggerUtil.java
package utils;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;

/**
 * 日志工具类
 */
public class LoggerUtil {
    static {
        // 配置日志
        String logConfigPath = "src/main/resources/log4j.properties";
        File file = new File(logConfigPath);
        if (file.exists()) {
            PropertyConfigurator.configure(logConfigPath);
        }
    }

    public static void debug(Logger logger, String format, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format(format, args));
        }
    }

    public static void info(Logger logger, String format, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format(format, args));
        }
    }

    public static void error(Logger logger, String format, Object... args) {
        logger.error(String.format(format, args));
    }

    public static void error(Logger logger, String format, Throwable t) {
        logger.error(format, t);
    }
}