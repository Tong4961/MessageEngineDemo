package com.me.bs.util;

import com.me.common.RequestCommon;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @ClassName RequestUtil
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/17 14:25
 * @Version 1.0
 */
@Slf4j
public class RequestUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static RequestCommon extractRequestCommon(HttpServletRequest request){
        RequestCommon requestCommon = RequestCommon.builder()
                .requestId(UUID.randomUUID().toString())
                .method(request.getMethod())
                .url(getFullURL(request))
                .clientIp(getClientIp(request))
                .createTime(LocalDateTime.now().format(FORMATTER))
                .build();
        try {
            requestCommon.setRequestHeaders(getHeadersJson(request));
            requestCommon.setRequestParams(getParamsJson(request));
            requestCommon.setRequestBody(getBody(request));
        } catch (Exception e) {
            log.error("get request info error: {}", e.getMessage());
            requestCommon.setRequestStatus("error");
            return requestCommon;
        }
        requestCommon.setRequestStatus("success");
        return requestCommon;
    }

    public static RequestCommon extractRequestCommon(HttpServletRequest request, String body){
        RequestCommon requestCommon = RequestCommon.builder()
                .requestId(UUID.randomUUID().toString())
                .method(request.getMethod())
                .url(getFullURL(request))
                .clientIp(getClientIp(request))
                .createTime(LocalDateTime.now().format(FORMATTER))
                .build();
        try {
            requestCommon.setRequestHeaders(getHeadersJson(request));
            requestCommon.setRequestParams(getParamsJson(request));
            requestCommon.setRequestBody(body);
        } catch (Exception e) {
            log.error("get request info error: {}", e.getMessage());
            requestCommon.setRequestStatus("error");
            return requestCommon;
        }
        requestCommon.setRequestStatus("success");
        return requestCommon;
    }

    private static String getHeadersJson(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return objectMapper.writeValueAsString(headers);
    }

    private static String getParamsJson(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<>();
        Map<String, String[]> paramMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            String[] values = entry.getValue();
            if (values.length == 1) {
                params.put(entry.getKey(), values[0]);
            } else {
                params.put(entry.getKey(), values);
            }
        }
        return objectMapper.writeValueAsString(params);
    }

    private static String getBody(HttpServletRequest request) throws Exception {
        ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (wrapper != null) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                return new String(buf, wrapper.getCharacterEncoding());
            }
        }
        // 如果不是包装的请求，尝试直接读取
        return request.getReader().lines().collect(Collectors.joining());
    }

    private static String getFullURL(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        if (request.getQueryString() != null) {
            url.append("?").append(request.getQueryString());
        }
        return url.toString();
    }

    private static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

}
