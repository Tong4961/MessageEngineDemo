package com.me.bp.process;

import com.me.common.RequestCommon;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @ClassName ExpressionResolver
 * @Description 自定义表达式解析器
 * @Author Ming
 * @Date 2026/4/28 16:22
 * @Version 1.0
 */
@Component
public class ExpressionResolver {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String resolve(String expression, ProcessContext processContext) {
        if (processContext==null || StringUtils.isEmpty(expression)) {
            return null;
        }
        if (expression.startsWith("request.")) {
            return extractValue(processContext.getRequest(), expression.replaceFirst("request.", ""));
        }
        if (expression.startsWith("context.")) {
            return processContext.getVariables().get(expression.replaceFirst("context.", ""));
        }
        if (expression.startsWith("response")) {
            return  processContext.getResponse();
        }
        return null;
    }

    /**
    *@author Ming
    *@Description 反射获取requestCommon属性值
    *@Date 2026/4/28 16:57
    */
    private String extractValue(RequestCommon requestCommon, String key) {
        if (requestCommon==null || StringUtils.isEmpty(key)) {
            return null;
        }
        try {
            Field field = RequestCommon.class.getDeclaredField(key);
            field.setAccessible(true);
            Object value = field.get(requestCommon);
            return value == null ? null : value.toString();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 字段不存在 / 无权限时返回 null
            return null;
        }
    }
}
