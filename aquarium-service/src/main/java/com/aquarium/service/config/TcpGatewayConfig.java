package com.aquarium.service.config;

import com.aquarium.service.tcp.TcpGatewayServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TcpGatewayConfig {

    private final TcpGatewayServer tcpGatewayServer;

    @Bean
    public CommandLineRunner startTcpGateway() {
        return args -> {
            try {
                tcpGatewayServer.start();
            } catch (Exception e) {
                log.error("Failed to start TCP Gateway", e);
            }
        };
    }
}
