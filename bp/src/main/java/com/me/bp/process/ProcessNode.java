package com.me.bp.process;

import lombok.Data;

/**
 * @ClassName ProcessNode
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/22 14:25
 * @Version 1.0
 */
@Data
public class ProcessNode {
    private String nodeId;
    private String nodeName;
    private String nodeType;
    private String nodeConfig;
    private String nextNodeId;
}
