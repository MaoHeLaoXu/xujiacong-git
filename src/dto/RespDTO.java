// src/main/java/dto/RespDTO.java
package dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 响应数据传输对象
 */
@Getter
@Setter
public class RespDTO {
    private RespStatusTypeEnum status;
    private String message;
    private Object data;

    public RespDTO(RespStatusTypeEnum status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // 成功响应
    public static RespDTO success(String message, Object data) {
        return new RespDTO(RespStatusTypeEnum.SUCCESS, message, data);
    }

    // 失败响应
    public static RespDTO fail(String message) {
        return new RespDTO(RespStatusTypeEnum.FAILURE, message, null);
    }
}
