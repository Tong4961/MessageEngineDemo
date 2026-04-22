package com.me.bs.soap;

import com.me.bs.common.RequestCommon;
import com.me.bs.common.ResponseResult;
import com.me.bs.common.RpcSyncContext;
import com.me.bs.util.RequestUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String syncWithTopic(String topic, String msg) {
        try {
            HttpServletRequest request = (HttpServletRequest) wsContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
            RequestCommon requestCommon = RequestUtil.extractRequestCommon(request, msg);
            requestCommon.setRequestType("soap");
            requestCommon.setRequestTopic(topic);
            String requestId = requestCommon.getRequestId();
            CompletableFuture<ResponseResult> responseFuture = RpcSyncContext.createRequest(requestId);
            SendReceipt sendReceipt = rocketMQClientTemplate.syncSendNormalMessage(topic, requestCommon);
            log.info("消息已发送, topic={}, requestId={}, messageId={}", topic, requestId, sendReceipt.getMessageId());
            //等待消费者返回结果（RPC同步调用）
            ResponseResult response = RpcSyncContext.getResponse(requestId);
            return OBJECT_MAPPER.writeValueAsString(response.getData());
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
