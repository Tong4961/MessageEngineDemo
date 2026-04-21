package com.me.demo.common;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

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
}