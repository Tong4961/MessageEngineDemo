package com.me.bp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.me.bp.entity.BO;
import com.me.bp.entity.HTTPConfig;
import com.me.bp.entity.HTTPServerInfo;
import com.me.bp.entity.ProcessNode;
import com.me.bp.mapper.BOMapper;
import com.me.bp.process.ExpressionResolver;
import com.me.bp.process.ProcessContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class HTTPService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Autowired
    private ExpressionResolver expressionResolver;
    @Autowired
    private RestClient restClient;
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
            RestClient.RequestBodyUriSpec spec = restClient.method(HttpMethod.valueOf(config.getMethod()));
            //1.获取url
            HTTPServerInfo serverInfo = OBJECT_MAPPER.readValue(bo.getConfig(), HTTPServerInfo.class);
            String fullUrl = serverInfo.getBaseUrl() + config.getUri();
            //2.获取payload
            String payload = expressionResolver.resolve(config.getPayloadExpression(), processContext);
            //3.获取header
            String header = expressionResolver.resolve(config.getHeaderExpression(), processContext);
            HttpHeaders headers = parseHeaders(header);
            HttpMediaType contentType = new HttpMediaType(config.getContentType());
            headers.setContentType(contentType);
            spec.headers(httpHeaders -> httpHeaders.addAll(headers));
            //4.http调用
            if ("body".equalsIgnoreCase(config.getParamLocation())) {
                spec.body(payload);
            }
            if ("form".equalsIgnoreCase(config.getParamLocation())) {
                spec.body(jsonToParamMap(payload));
            }
            if ("param".equalsIgnoreCase(config.getParamLocation())) {
                fullUrl += payload;
            }
            spec.uri(fullUrl);
            ResponseEntity<String> response = spec.retrieve().toEntity(String.class);
            String respBody = response.getBody();
            //5.响应存入context
            if (config.getResponseExpression().startsWith("context.")) {
                processContext.getVariables().put(config.getResponseExpression().replace("context.", ""), respBody);
            } else if (config.getResponseExpression().equals("response")) {
                processContext.setResponse(respBody);
            }
            log.info("HTTP调用成功, url={}, result={}", fullUrl, respBody);
        } catch (Exception e) {
            log.error("HTTP调用失败, nodeId={}, error={}", node.getNodeId(), e.getMessage());
            processContext.getVariables().put("httpResponse_" + node.getNodeId(), null);
        }
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

    public static MultiValueMap<String, String> jsonToParamMap(String json) {
        try {
            Map<String, Object> tempMap = OBJECT_MAPPER.readValue(json,OBJECT_MAPPER.getTypeFactory().constructType(Map.class));
            Map<String, String[]> resultMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : tempMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof List<?>) {
                    List<?> list = (List<?>) value;
                    String[] arr = new String[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        arr[i] = list.get(i) == null ? null : list.get(i).toString();
                    }
                    resultMap.put(key, arr);
                } else {
                    resultMap.put(key, new String[]{value == null ? "" : value.toString()});
                }
            }
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            for (Map.Entry<String, String[]> entry : resultMap.entrySet()) {
                String key = entry.getKey();
                for (String val : entry.getValue()) {
                    formData.add(key, val);
                }
            }
            return formData;
        } catch (Exception e) {
            return new LinkedMultiValueMap<>();
        }
    }
}
