package com.me.bp.process;

import lombok.Data;
import java.util.List;

/**
 * @ClassName ProcessConfig
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/22 14:34
 * @Version 1.0
 */
@Data
public class ProcessConfig {
    private String processId;
    private String processName;
    private List<ProcessNode> processNodes;
}
