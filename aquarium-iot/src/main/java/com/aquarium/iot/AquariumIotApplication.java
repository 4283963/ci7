package com.aquarium.iot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.aquarium")
@MapperScan("com.aquarium.iot.mapper")
public class AquariumIotApplication {
    public static void main(String[] args) {
        SpringApplication.run(AquariumIotApplication.class, args);
    }
}
