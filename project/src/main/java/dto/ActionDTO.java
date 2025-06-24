package dto;

import model.command.AbstractCommand;

/**
 * 动作数据传输对象
 */
public class ActionDTO {
    private AbstractCommand command;

    public ActionDTO(AbstractCommand command) {
        this.command = command;
    }

    public AbstractCommand getCommand() {
        return command;
    }

    public void setCommand(AbstractCommand command) {
        this.command = command;
    }
}