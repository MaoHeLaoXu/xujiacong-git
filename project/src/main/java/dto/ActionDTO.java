package dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class ActionDTO implements Serializable {
    private ActionTypeEnum type;
    private String key;
    private String value;

    public ActionDTO(ActionTypeEnum type, String key, String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "ActionDTO{" +
                "type=" + type +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
