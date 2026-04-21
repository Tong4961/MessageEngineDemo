package com.me.demo.bs;

import com.me.demo.common.RequestCommon;
import com.me.demo.common.RequestUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.concurrent.CompletableFuture;

/**
 * @ClassName SOAPServiceImpl
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/20 10:37
 * @Version 1.0
 */
@Service
public class SOAPServiceImpl implements SOAPService {
    @Resource
    private WebServiceContext wsContext;
    @Autowired
    private RocketMQClientTemplate rocketMQClientTemplate;

    @Override
    public String syncWithTopic(String topic, String msg) {
        try {
            HttpServletRequest request = (HttpServletRequest) wsContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
            RequestCommon requestCommon = RequestUtil.extractRequestCommon(request, msg);
            requestCommon.setRequestType("soap");
            requestCommon.setRequestTopic(topic);
            SendReceipt sendReceipt = rocketMQClientTemplate.syncSendNormalMessage(topic, requestCommon);
            return """
                {"code": 200,"data": "Message received. Success."}
                """;
        } catch (Exception e) {
            return """
                    {"code": 500,"data": "Message received. Error. %s"}
                    """.formatted(e.getMessage());
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
            return """
                    {"code": 500,"data": "Message received. Error. %s"}
                    """.formatted(e.getMessage());
        }
        return """
                {"code": 200,"data": "Message received. Success."}
                """;
    }

    @Override
    public String syncWithUrl(String msg) {
        HttpServletRequest request = (HttpServletRequest) wsContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
        String topic = request.getParameter("topic");
        if (StringUtils.isEmpty(topic)) {
            return """
                {"code": 400,"data": "Message received. No Topic."}
                """;
        }
        System.out.println("topic:"+topic);
        return syncWithTopic(topic,msg);
    }

    @Override
    public String asyncWithUrl(String msg) {
        HttpServletRequest request = (HttpServletRequest) wsContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
        String topic = request.getParameter("topic");
        if (StringUtils.isEmpty(topic)) {
            return """
                {"code": 400,"data": "Message received. No Topic."}
                """;
        }
        System.out.println("topic:"+topic);
        return asyncWithTopic(topic,msg);
    }
}
