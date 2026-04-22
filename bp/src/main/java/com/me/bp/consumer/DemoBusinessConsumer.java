package com.me.bp.consumer;

import com.me.bp.common.RequestCommon;
import com.me.bp.common.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模拟外部消费者（另一项目）
 * 消费业务消息后，发送 reply 到 meDemo_reply topic
 */
@Slf4j
@Service
public class DemoBusinessConsumer implements DisposableBean {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private PushConsumer pushConsumer;
    private Producer producer;

    @Value("${rocketmq.push-consumer.endpoints}")
    private String proxyAddr;

    @Value("${rocketmq.reply-consumer.default-topic:meDemo_reply}")
    private String replyTopic;

    /**
     * 初始化模拟消费者
     */
    public void init(String topics) {
        try {
            ClientServiceProvider provider = ClientServiceProvider.loadService();

            // 初始化 Producer 用于发送 reply
            ClientConfiguration producerConfig = ClientConfiguration.newBuilder()
                    .setEndpoints(this.proxyAddr)
                    .enableSsl(false)
                    .build();
            this.producer = provider.newProducerBuilder()
                    .setClientConfiguration(producerConfig)
                    .build();

            // 初始化 PushConsumer 订阅业务 topic
            FilterExpression filterExpression = new FilterExpression("*", FilterExpressionType.TAG);
            Map<String, FilterExpression> subs = Arrays.stream(topics.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toMap(topic -> topic, topic -> filterExpression));

            ClientConfiguration consumerConfig = ClientConfiguration.newBuilder()
                    .setEndpoints(this.proxyAddr)
                    .enableSsl(false)
                    .build();

            this.pushConsumer = provider.newPushConsumerBuilder()
                    .setConsumerGroup("meDemoBusinessConsumerGroup")
                    .setClientConfiguration(consumerConfig)
                    .setSubscriptionExpressions(subs)
                    .setMessageListener(messageView -> {
                        try {
                            String body = StandardCharsets.UTF_8.decode(messageView.getBody()).toString();
                            RequestCommon request = OBJECT_MAPPER.readValue(body, RequestCommon.class);
                            log.info("[模拟消费者] 收到消息: Topic={}, MessageId={}, requestId={}",
                                    messageView.getTopic(), messageView.getMessageId(), request.getRequestId());

                            // 模拟业务处理
                            Thread.sleep(500);

                            // 发送 reply 到 meDemo_reply topic
                            ResponseResult reply = ResponseResult.builder()
                                    .requestId(request.getRequestId())
                                    .code(200)
                                    .message("处理完成")
                                    .data("reply from business consumer, messageId=" + messageView.getMessageId())
                                    .timestamp(System.currentTimeMillis())
                                    .build();

                            String replyBody = OBJECT_MAPPER.writeValueAsString(reply);
                            Message replyMsg = provider.newMessageBuilder()
                                    .setTopic(replyTopic)
                                    .setBody(replyBody.getBytes(StandardCharsets.UTF_8))
                                    .setTag("reply")
                                    .build();
                            this.producer.send(replyMsg);
                            log.info("[模拟消费者] 已发送 reply: topic={}, requestId={}", replyTopic, request.getRequestId());

                            return ConsumeResult.SUCCESS;
                        } catch (Exception e) {
                            log.error("[模拟消费者] 处理消息异常: {}", e.getMessage());
                            return ConsumeResult.FAILURE;
                        }
                    })
                    .build();

            log.info("DemoBusinessConsumer 启动成功，订阅 topics={}", topics);
        } catch (Exception e) {
            log.error("DemoBusinessConsumer 启动失败: {}", e.getMessage());
        }
    }

    @Override
    public void destroy() throws Exception {
        if (this.pushConsumer != null) {
            this.pushConsumer.close();
        }
        if (this.producer != null) {
            this.producer.close();
        }
        log.info("DemoBusinessConsumer 已关闭");
    }
}
