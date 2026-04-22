package com.me.bp.consumer;

import com.me.bp.common.RequestCommon;
import com.me.bp.common.ResponseResult;
import com.me.bp.process.ProcessEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName AllTopicConsumer
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/20 14:20
 * @Version 1.0
 */
@Slf4j
@Service
public class AllTopicConsumer implements DisposableBean {
    @Autowired
    private RocketMQClientTemplate rocketMQClientTemplate;
    @Autowired
    private ProcessEngine processEngine;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private PushConsumer pushConsumer;
    Map<String, FilterExpression> subscriptionExpressions = new HashMap<>();
    @Value("${rocketmq.push-consumer.endpoints}")
    private String proxyAddr;

    /**
    *@author Ming
    *@Description 初始化PushConsumer
    *@Date 2026/4/20 17:16
    */
    public void initPushConsumer(String topics) {
        try {
            if (StringUtils.isEmpty(topics)) {
                log.error("BP AllTopicConsumer 启动失败 topic为空");
                return;
            }
            FilterExpression filterExpression = new FilterExpression("*", FilterExpressionType.TAG);
            this.subscriptionExpressions = Arrays.stream(topics.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toMap(topic -> topic, topic -> filterExpression));
            ClientServiceProvider provider = ClientServiceProvider.loadService();
            ClientConfiguration configuration = ClientConfiguration.newBuilder()
                    .setEndpoints(this.proxyAddr)
                    .enableSsl(false)
                    .build();
            this.pushConsumer = provider.newPushConsumerBuilder()
                    .setConsumerGroup("BPAllTopicConsumerGroup")
                    .setClientConfiguration(configuration)
                    .setSubscriptionExpressions(this.subscriptionExpressions)
                    .setMessageListener(messageView -> {
                        try {
                            String body = StandardCharsets.UTF_8.decode(messageView.getBody()).toString();
                            RequestCommon requestCommon = OBJECT_MAPPER.readValue(body, RequestCommon.class);
                            System.out.println("收到消息: Topic=" + messageView.getTopic() + ", MessageId=" + messageView.getMessageId()+ ", MessageBody=" + requestCommon);
                            ResponseResult reply = processEngine.executeTest(messageView.getTopic(), requestCommon);
                            if ("sync".equals(requestCommon.getSyncType())) {
                                rocketMQClientTemplate.asyncSendNormalMessage("replyTopic", reply, null);
                                log.info("同步消息响应已返回MQ topic={} , requestId={}", "replyTopic", requestCommon.getRequestId());
                            } else {
                                log.info("异步消息无需回复: topic={}, requestId={}", messageView.getTopic(), requestCommon.getRequestId());
                            }
                            return ConsumeResult.SUCCESS;
                        } catch (Exception e) {
                            e.printStackTrace();
                            log.error("BP AllTopicConsumer topic={} err:{}", messageView.getTopic(), e.getMessage());
                            return ConsumeResult.FAILURE;
                        }
                    })
                    .build();
            log.info("BP AllTopicConsumer 启动成功");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("BP AllTopicConsumer 启动失败 error: {}", e.getMessage());
        }
    }

    /**
    *@author Ming
    *@Description 增加订阅
    *@Date 2026/4/20 17:16
    */
    public synchronized void subscribeTopic(String topic) throws Exception {
        if (this.subscriptionExpressions.containsKey(topic)) {
            log.warn("BP AllTopicConsumer 已经订阅 Topic {}", topic);
            return;
        }
        FilterExpression expression = new FilterExpression("*", FilterExpressionType.TAG);
        this.subscriptionExpressions.put(topic.trim(), expression);
        this.pushConsumer.subscribe(topic, expression);
        log.info("BP AllTopicConsumer 成功订阅 Topic: {}", topic);
    }

    /**
    *@author Ming
    *@Description 取消订阅
    *@Date 2026/4/20 17:16
    */
    public synchronized void unsubscribeTopic(String topic) throws Exception {
        if (!this.subscriptionExpressions.containsKey(topic)) {
            log.warn("BP AllTopicConsumer 未订阅 Topic {} ", topic);
            return;
        }
        this.subscriptionExpressions.remove(topic);
        this.pushConsumer.unsubscribe(topic);
        log.info("BP AllTopicConsumer 取消订阅 Topic: {}", topic);
    }

    public boolean isRunning() {
        return this.pushConsumer != null;
    }

    @Override
    public void destroy() throws Exception {
        if (this.pushConsumer != null) {
            this.pushConsumer.close();
            log.info("BP RocketMQ AllTopicConsumer 已关闭");
        }
    }
}
