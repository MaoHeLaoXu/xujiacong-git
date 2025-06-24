package model.command;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CommandPos {
    private int pos;
    private int len;

    public CommandPos(int pos, int len) {
        this.pos = pos;
        this.len = len;
    }

    @Override
    public String toString() {
        return "CommandPos{" +
                "pos=" + pos +
                ", len=" + len +
                '}';
    }
}
