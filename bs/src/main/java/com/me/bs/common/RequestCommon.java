package com.me.bs.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * @ClassName RequestCommon
 * @Description 普通请求对象
 * @Author Ming
 * @Date 2026/4/17 14:11
 * @Version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCommon {
    private String requestId;
    private String requestTopic;
    private String requestStatus; //success error
    private String requestHeaders; //json格式
    private String requestParams; //json格式
    private String requestBody;
    private String requestType; //http soap tcp socket
    private String syncType;
    private String url;
    private String method;
    private String clientIp;
    private String createTime;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String toString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            return "RequestCommon toString error：" + e.getMessage();
        }
    }

}
