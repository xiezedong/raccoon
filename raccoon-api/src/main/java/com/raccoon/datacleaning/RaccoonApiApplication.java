package com.raccoon.datacleaning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Raccoon 数据清洗工具主应用
 */
@SpringBootApplication
@EnableScheduling
public class RaccoonApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RaccoonApiApplication.class, args);
    }
}
