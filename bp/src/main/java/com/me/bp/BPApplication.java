package com.me.bp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.me.bp.mapper")
public class BPApplication {

    public static void main(String[] args) {
        SpringApplication.run(BPApplication.class, args);
    }

}
