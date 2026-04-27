package com.me.bp.service;

import com.me.bp.entity.BO;
import com.me.bp.entity.HTTPConfig;
import com.me.bp.mapper.BOMapper;
import com.me.bp.process.ProcessContext;
import com.me.bp.entity.ProcessNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;

@Slf4j
@Service
public class HTTPService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private BOMapper boMapper;

    public void doHTTPInvoking(ProcessContext processContext, ProcessNode node) {
        try {
            HTTPConfig config = OBJECT_MAPPER.readValue(node.getNodeConfig(), HTTPConfig.class);
            BO bo = boMapper.selectOneById(config.getBoId());
            if (bo == null) {
                log.error("BO not found, boId={}", config.getBoId());
                return;
            }

            ServerInfo serverInfo = OBJECT_MAPPER.readValue(bo.getConfig(), ServerInfo.class);
            String baseUrl = serverInfo.getProtocol() + "://" + serverInfo.getIp() + ":" + serverInfo.getPort();
            String fullUrl = baseUrl + config.getUri();

            Object payload = resolvePayload(config.getPayload(), processContext);

            RestClient restClient = RestClient.create();
            String result;

            HttpHeaders headers = parseHeaders(config.getHeader());
            HttpMediaType contentType = new HttpMediaType(config.getContentType());

            if ("GET".equalsIgnoreCase(config.getMethod())) {
                String getUrl = fullUrl;
                if (payload != null) {
                    getUrl = fullUrl + "?payload=" + payload;
                }
                ResponseEntity<String> response = restClient.get()
                        .uri(getUrl)
                        .headers(h -> h.addAll(headers))
                        .retrieve()
                        .toEntity(String.class);
                result = response.getBody();
            } else if ("POST".equalsIgnoreCase(config.getMethod())) {
                HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
                ResponseEntity<String> response = restClient.post()
                        .uri(fullUrl)
                        .contentType(contentType)
                        .body(payload)
                        .retrieve()
                        .toEntity(String.class);
                result = response.getBody();
            } else if ("PUT".equalsIgnoreCase(config.getMethod())) {
                HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
                ResponseEntity<String> response = restClient.put()
                        .uri(fullUrl)
                        .contentType(contentType)
                        .body(payload)
                        .retrieve()
                        .toEntity(String.class);
                result = response.getBody();
            } else if ("DELETE".equalsIgnoreCase(config.getMethod())) {
                ResponseEntity<String> response = restClient.delete()
                        .uri(fullUrl)
                        .headers(h -> h.addAll(headers))
                        .retrieve()
                        .toEntity(String.class);
                result = response.getBody();
            } else {
                log.warn("不支持的HTTP方法, method={}", config.getMethod());
                result = null;
            }

            log.info("HTTP调用成功, url={}, result={}", fullUrl, result);
            processContext.getVariables().put("httpResponse_" + node.getNodeId(), result);

        } catch (Exception e) {
            log.error("HTTP调用失败, nodeId={}, error={}", node.getNodeId(), e.getMessage());
            processContext.getVariables().put("httpResponse_" + node.getNodeId(), null);
        }
    }

    private Object resolvePayload(String payloadTemplate, ProcessContext processContext) {
        if (StringUtils.isEmpty(payloadTemplate)) {
            return null;
        }
        Map<String, Object> variables = processContext.getVariables();
        if (variables == null || variables.isEmpty()) {
            return payloadTemplate;
        }
        String resolved = payloadTemplate;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            resolved = resolved.replace("context." + entry.getKey(), String.valueOf(entry.getValue()));
        }
        return resolved;
    }

    private HttpHeaders parseHeaders(String headerJson) {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.isEmpty(headerJson)) {
            return headers;
        }
        try {
            Map<String, String> headerMap = OBJECT_MAPPER.readValue(headerJson, Map.class);
            headerMap.forEach(headers::add);
        } catch (Exception e) {
            log.warn("解析header失败, headerJson={}, error={}", headerJson, e.getMessage());
        }
        return headers;
    }

    @Data
    public static class ServerInfo {
        private String ip;
        private int port;
        private String protocol = "http";
        private int timeout = 30000;
    }

    @Data
    public static class HttpMediaType extends MediaType {
        public HttpMediaType(String mediaType) {
            super(getMainType(mediaType), getSubType(mediaType));
        }

        private static String getMainType(String mediaType) {
            if (mediaType == null) return "application";
            int slash = mediaType.indexOf('/');
            return slash > 0 ? mediaType.substring(0, slash) : "application";
        }

        private static String getSubType(String mediaType) {
            if (mediaType == null) return "json";
            int slash = mediaType.indexOf('/');
            return slash > 0 ? mediaType.substring(slash + 1) : "json";
        }
    }
}
