package com.me.bp.entity;

import lombok.Data;

import static org.yaml.snakeyaml.nodes.Tag.STR;

/**
 * @ClassName ServerInfo
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/28 15:38
 * @Version 1.0
 */
@Data
public class HTTPServerInfo {
    private String host;
    private int port;
    private String protocol;// http/https
    private int timeout;// seconds

    public String getBaseUrl(){
        return String.format("%s://%s:%d", protocol, host, port);
    }
}
