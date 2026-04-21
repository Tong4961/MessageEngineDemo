package com.me.demo.bp;

import com.me.demo.common.ResponseResult;
import com.me.demo.common.RpcSyncContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * 回复消息消费者
 * 订阅 reply topic，收到消费者（另一项目）的回复后，唤醒 RpcSyncContext
 */
@Slf4j
@Service
public class ReplyConsumer implements DisposableBean {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private PushConsumer pushConsumer;

    @Value("${rocketmq.push-consumer.endpoints}")
    private String proxyAddr;

    @Value("${rocketmq.reply-consumer.default-topic:meDemo_reply}")
    private String defaultReplyTopic;

    /**
     * 初始化 reply topic 消费者
     */
    public void init() {
        try {
            ClientServiceProvider provider = ClientServiceProvider.loadService();
            ClientConfiguration configuration = ClientConfiguration.newBuilder()
                    .setEndpoints(this.proxyAddr)
                    .enableSsl(false)
                    .build();

            FilterExpression filterExpression = new FilterExpression("*", FilterExpressionType.TAG);

            this.pushConsumer = provider.newPushConsumerBuilder()
                    .setConsumerGroup("meDemoReplyConsumerGroup")
                    .setClientConfiguration(configuration)
                    .setSubscriptionExpressions(java.util.Map.of(defaultReplyTopic, filterExpression))
                    .setMessageListener(messageView -> {
                        try {
                            String body = StandardCharsets.UTF_8.decode(messageView.getBody()).toString();
                            ResponseResult response = OBJECT_MAPPER.readValue(body, ResponseResult.class);
                            String requestId = response.getRequestId();
                            log.info("收到回复消息: topic={}, requestId={}, code={}",
                                    messageView.getTopic(), requestId, response.getCode());

                            if (requestId != null) {
                                RpcSyncContext.onResponse(requestId, response);
                            }
                            return ConsumeResult.SUCCESS;
                        } catch (Exception e) {
                            log.error("处理回复消息异常: {}", e.getMessage());
                            return ConsumeResult.FAILURE;
                        }
                    })
                    .build();

            log.info("ReplyConsumer 启动成功，订阅 topic={}", defaultReplyTopic);
        } catch (Exception e) {
            log.error("ReplyConsumer 启动失败: {}", e.getMessage());
        }
    }

    @Override
    public void destroy() throws Exception {
        if (this.pushConsumer != null) {
            this.pushConsumer.close();
            log.info("ReplyConsumer 已关闭");
        }
    }
}
