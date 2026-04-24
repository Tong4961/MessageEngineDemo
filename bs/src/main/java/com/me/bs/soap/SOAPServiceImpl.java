package com.me.bs.soap;

import com.me.bs.util.RequestUtil;
import com.me.common.RequestCommon;
import com.me.common.ResponseResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;

/**
 * @ClassName SOAPServiceImpl
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/20 10:37
 * @Version 1.0
 */
@Slf4j
@Service
public class SOAPServiceImpl implements SOAPService {
    @Resource
    private WebServiceContext wsContext;
    @Autowired
    private RocketMQClientTemplate rocketMQClientTemplate;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String syncWithTopic(String topic, String msg) {
        try {
            HttpServletRequest request = (HttpServletRequest) wsContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
            RequestCommon requestCommon = RequestUtil.extractRequestCommon(request, msg);
            requestCommon.setRequestType("soap");
            requestCommon.setSyncType("sync");
            requestCommon.setRequestTopic(topic);
            String requestId = requestCommon.getRequestId();
            SendReceipt sendReceipt = rocketMQClientTemplate.syncSendNormalMessage(topic, requestCommon);
            log.info("消息已发送, topic={}, requestId={}, messageId={}", topic, requestId, sendReceipt.getMessageId());
            //后期使用redissonClient进行优化 弃用redisTemplate
            long startTime = System.currentTimeMillis();
            long timeoutMs = 60000;
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                Object reply = redisTemplate.opsForValue().get("reply:" + requestId);
                if (reply != null) {
                    ResponseResult result = OBJECT_MAPPER.readValue((String) reply, ResponseResult.class);
                    log.info("从Redis获取响应成功, requestId={}", requestId);
                    return result.toString();
                }
                Thread.sleep(100);
            }
            log.warn("从Redis获取响应超时, requestId={}", requestId);
            return ResponseResult.error("timeout").toString();
        } catch (Exception e) {
            log.error("syncMethod error: {}", e.getMessage());
            return ResponseResult.error(e.getMessage()).toString();
        }
    }

    @Override
    public String asyncWithTopic(String topic, String msg) {
        try {
            HttpServletRequest request = (HttpServletRequest) wsContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
            RequestCommon requestCommon = RequestUtil.extractRequestCommon(request, msg);
            requestCommon.setRequestType("soap");
            requestCommon.setSyncType("async");
            requestCommon.setRequestTopic(topic);
            CompletableFuture<SendReceipt> future = rocketMQClientTemplate.asyncSendNormalMessage(topic, requestCommon, null);
            future.whenComplete((sendReceipt, throwable) -> {
                if (throwable != null) {
                    System.out.println("send MQ fail: Topic=" +topic + ", MessageId=" + requestCommon.getRequestId());
                } else {
                    System.out.println("send MQ success: Topic=" +topic + ", MessageId=" + requestCommon.getRequestId());
                }
            });
        } catch (Exception e) {
            log.error("asyncMethod error: {}", e.getMessage());
            return ResponseResult.error(e.getMessage()).toString();
        }
        return ResponseResult.success("").toString();
    }

    @Override
    public String syncWithUrl(String msg) {
        HttpServletRequest request = (HttpServletRequest) wsContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
        String topic = request.getParameter("topic");
        if (StringUtils.isEmpty(topic)) {
            return ResponseResult.error("No Topic").toString();
        }
        return syncWithTopic(topic,msg);
    }

    @Override
    public String asyncWithUrl(String msg) {
        HttpServletRequest request = (HttpServletRequest) wsContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
        String topic = request.getParameter("topic");
        if (StringUtils.isEmpty(topic)) {
            return ResponseResult.error("No Topic").toString();
        }
        return asyncWithTopic(topic,msg);
    }
}
