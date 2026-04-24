package com.me.bs.consumer;

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
    private ReplyConsumer replyConsumer;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("SpringBoot启动完毕 Create Consumer...");
        replyConsumer.init();
    }
}
