package com.me.bs.http;

import com.me.bs.util.RequestUtil;
import com.me.common.RequestCommon;
import com.me.common.ResponseResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    //@Autowired
    //private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
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
            return ResponseResult.success(requestCommon.getRequestId(), "").toString();
        } catch (Exception e) {
            log.error("asyncMethod error: {}", e.getMessage());
            return ResponseResult.error(e.getMessage()).toString();
        }
    }
}
