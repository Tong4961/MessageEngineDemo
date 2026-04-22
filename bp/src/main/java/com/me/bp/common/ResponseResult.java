package com.me.bp.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * RPC响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseResult {
    private String requestId;
    private Integer code;
    private String message;
    private Object data;
    private Long timestamp;

    public static ResponseResult success(Object data) {
        return ResponseResult.builder()
                .code(200)
                .message("success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static ResponseResult error(String message) {
        return ResponseResult.builder()
                .code(500)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(); //全局静态ObjectMapper 只创建一次性能好

    @Override
    public String toString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            return "RequestCommon toString error：" + e.getMessage();
        }
    }
}