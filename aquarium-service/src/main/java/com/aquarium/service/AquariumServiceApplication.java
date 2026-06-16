package com.aquarium.service;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.aquarium")
@MapperScan("com.aquarium.service.mapper")
@EnableCaching
@EnableScheduling
public class AquariumServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AquariumServiceApplication.class, args);
    }
}
