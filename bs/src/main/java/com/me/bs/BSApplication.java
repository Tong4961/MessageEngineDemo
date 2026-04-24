package com.me.bs;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.me.bs.mapper")
public class BSApplication {
    public static void main(String[] args) {
        SpringApplication.run(BSApplication.class, args);
    }
}
