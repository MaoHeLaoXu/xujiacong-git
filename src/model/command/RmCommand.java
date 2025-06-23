// src/main/java/model/command/RmCommand.java
package model.command;

import lombok.Getter;
import lombok.Setter;

/**
 * 删除命令
 */
@Getter
@Setter
public class RmCommand extends AbstractCommand {
    private String key;

    public RmCommand(String key) {
        super(CommandTypeEnum.RM);
        this.key = key;
    }
}
