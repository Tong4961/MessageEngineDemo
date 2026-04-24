package com.me.bs.http;

import com.me.bs.util.RequestUtil;
import com.me.common.RequestCommon;
import com.me.common.ResponseResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;

/**
 * @ClassName HTTPController
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/17 13:25
 * @Version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/http")
public class HTTPController {
    @Autowired
    private RocketMQClientTemplate rocketMQClientTemplate;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /**
    *@author Ming
    *@Description POST同步 直接返回BP处理后的消息
    *@Date 2026/4/17 14:01
    */
    @PostMapping("/postsync/{topic}")
    public String postsync(@PathVariable String topic, HttpServletRequest request){
        return syncMethod(topic, request);
    }

    /**
    *@author Ming
    *@Description POST异步
    *@Date 2026/4/17 14:01
    */
    @PostMapping("/postasync/{topic}")
    public String postasync(@PathVariable String topic, HttpServletRequest request){
        return asyncMethod(topic, request);
    }

    /**
    *@author Ming
    *@Description GET同步 直接返回BP处理后的消息
    *@Date 2026/4/17 14:02
    */
    @GetMapping("/getsync/{topic}")
    public String getsync(@PathVariable String topic, HttpServletRequest request){
        return syncMethod(topic, request);
    }

    /**
    *@author Ming
    *@Description GET异步
    *@Date 2026/4/17 14:03
    */
    @GetMapping("/getasync/{topic}")
    public String getasync(@PathVariable String topic, HttpServletRequest request){
        return asyncMethod(topic, request);
    }

    private String syncMethod(String topic, HttpServletRequest request){
        try {
            RequestCommon requestCommon = RequestUtil.extractRequestCommon(request);
            requestCommon.setRequestType("http");
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

    private String asyncMethod(String topic, HttpServletRequest request){
        try {
            RequestCommon requestCommon = RequestUtil.extractRequestCommon(request);
            requestCommon.setRequestType("http");
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
}
