package com.me.demo.bp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @ClassName ConsumerRunner
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/20 14:28
 * @Version 1.0
 */
@Component
public class ConsumerRunner implements CommandLineRunner {
    @Autowired
    private AllTopicConsumer consumerService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("SpringBoot启动完毕 Create Consumer...");
        consumerService.initPushConsumer("JH0001,JH0002,JH0003");
    }
}
