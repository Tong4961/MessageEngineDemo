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
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    //@Autowired
    //private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
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
            RBlockingQueue<String> queue = redissonClient.getBlockingQueue("reply:" + requestId);
            redissonClient.getKeys().expire(queue.getName(), 60+10, TimeUnit.SECONDS);
            String replyJson = queue.poll(60, TimeUnit.SECONDS);
            if (replyJson != null) {
                ResponseResult result = OBJECT_MAPPER.readValue(replyJson, ResponseResult.class);
                log.info("从Redis获取响应成功, requestId={}", requestId);
                return result.toString();
            }
            log.warn("从Redis获取响应超时, requestId={}", requestId);
            return ResponseResult.error(requestCommon.getRequestId(), "timeout").toString();
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
            return ResponseResult.success(requestCommon.getRequestId(), "").toString();
        } catch (Exception e) {
            log.error("asyncMethod error: {}", e.getMessage());
            return ResponseResult.error(e.getMessage()).toString();
        }
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
