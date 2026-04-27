package com.me.bp.process;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName ProcessContext
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/22 14:22
 * @Version 1.0
 */
@Data
public class ProcessContext {
    private String processId;
    private Map<String, Object> variables = new HashMap<>();
}
