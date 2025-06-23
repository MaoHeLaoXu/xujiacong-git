// src/main/java/model/command/AbstractCommand.java
package model.command;

import lombok.Getter;

/**
 * 抽象命令类，所有命令的基类
 */
@Getter
public abstract class AbstractCommand {
    private final CommandTypeEnum type;

    public AbstractCommand(CommandTypeEnum type) {
        this.type = type;
    }

    public abstract String getKey();
}