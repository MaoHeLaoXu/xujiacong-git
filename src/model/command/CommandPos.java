// src/main/java/model/command/CommandPos.java
package model.command;

import lombok.Getter;
import lombok.Setter;

/**
 * 命令在日志文件中的位置信息
 */
@Getter
@Setter
public class CommandPos {
    private int pos;  // 偏移量
    private int len;  // 长度

    public CommandPos(int pos, int len) {
        this.pos = pos;
        this.len = len;
    }
}
