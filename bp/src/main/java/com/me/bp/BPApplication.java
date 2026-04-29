package com.me.bp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@ComponentScan(basePackages = {"com.me.bp", "com.me.common.config"})
@MapperScan("com.me.bp.mapper")
public class BPApplication {

    public static void main(String[] args) {
        SpringApplication.run(BPApplication.class, args);
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }

}
