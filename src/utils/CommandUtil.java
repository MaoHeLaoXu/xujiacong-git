// src/main/java/utils/CommandUtil.java
package utils;

import com.alibaba.fastjson.JSONObject;
import model.command.AbstractCommand;
import model.command.CommandTypeEnum;
import model.command.RmCommand;
import model.command.SetCommand;

/**
 * 命令工具类
 */
public class CommandUtil {
    /**
     * 将JSON对象转换为命令对象
     */
    public static AbstractCommand jsonToCommand(JSONObject json) {
        if (json == null) {
            return null;
        }

        String type = json.getString("type");
        if (type == null) {
            return null;
        }

        CommandTypeEnum commandType = CommandTypeEnum.valueOf(type);
        String key = json.getString("key");

        switch (commandType) {
            case SET:
                String value = json.getString("value");
                return new SetCommand(key, value);
            case RM:
                return new RmCommand(key);
            default:
                return null;
        }
    }
}