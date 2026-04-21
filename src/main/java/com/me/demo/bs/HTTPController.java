package com.me.demo.bs;

import com.me.demo.common.RequestCommon;
import com.me.demo.common.RequestUtil;
import com.me.demo.common.ResponseResult;
import com.me.demo.common.RpcSyncContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;

import tools.jackson.databind.ObjectMapper;

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
            requestCommon.setRequestTopic(topic);

            //创建RPC同步上下文，注册future用于接收消费者回调
            String requestId = requestCommon.getRequestId();
            CompletableFuture<ResponseResult> responseFuture = RpcSyncContext.createRequest(requestId);

            //发送消息
            SendReceipt sendReceipt = rocketMQClientTemplate.syncSendNormalMessage(topic, requestCommon);
            log.info("消息已发送, topic={}, requestId={}, messageId={}", topic, requestId, sendReceipt.getMessageId());

            //等待消费者返回结果（RPC同步调用）
            ResponseResult response = RpcSyncContext.getResponse(requestId);
            log.info("收到消费者响应, requestId={}, code={}, message={}", requestId, response.getCode(), response.getMessage());

            return new ObjectMapper().writeValueAsString(response);
        } catch (Exception e) {
            log.error("syncMethod error: {}", e.getMessage());
            return new ObjectMapper().writeValueAsString(ResponseResult.error(e.getMessage()));
        }
    }

    private String asyncMethod(String topic, HttpServletRequest request){
        try {
            RequestCommon requestCommon = RequestUtil.extractRequestCommon(request);
            requestCommon.setRequestType("http");
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
            log.error("getasync error: {}", e.getMessage());
            return """
                    {"code": 500,"data": "Message received. Error. %s"}
                    """.formatted(e.getMessage());
        }
        return """
                {"code": 200,"data": "Message received. Success."}
                """;
    }
}
