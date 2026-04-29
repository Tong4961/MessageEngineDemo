package com.me.bp.entity;

import lombok.Data;

/**
 * @ClassName HTTPConfig
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/24 14:57
 * @Version 1.0
 */
@Data
public class HTTPConfig {
    private int boId;
    private String uri;
    private String method;// GET/POST/PUT/DELETE
    private String contentType;// json/form/xml/txt
    private String paramLocation;// body/form/param
    private String headerExpression;// context.header
    private String payloadExpression;// context.jsonstr/request.requestBody/request.requestParams/request.queryString
    private String responseExpression;// context.rep/response
}
