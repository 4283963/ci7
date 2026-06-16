package com.aquarium.service.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange deviceCommandExchange() {
        return new TopicExchange("aquarium.device.command", true, false);
    }

    @Bean
    public TopicExchange sensorDataExchange() {
        return new TopicExchange("aquarium.sensor.data", true, false);
    }

    @Bean
    public TopicExchange alertExchange() {
        return new TopicExchange("aquarium.alert", true, false);
    }

    @Bean
    public Queue sensorDataQueue() {
        return QueueBuilder.durable("aquarium.queue.sensor.data").build();
    }

    @Bean
    public Queue deviceCommandQueue() {
        return QueueBuilder.durable("aquarium.queue.device.command").build();
    }

    @Bean
    public Queue alertQueue() {
        return QueueBuilder.durable("aquarium.queue.alert").build();
    }

    @Bean
    public Binding sensorDataBinding() {
        return BindingBuilder.bind(sensorDataQueue())
                .to(sensorDataExchange())
                .with("sensor.data.#");
    }

    @Bean
    public Binding deviceCommandBinding() {
        return BindingBuilder.bind(deviceCommandQueue())
                .to(deviceCommandExchange())
                .with("device.command.#");
    }

    @Bean
    public Binding alertBinding() {
        return BindingBuilder.bind(alertQueue())
                .to(alertExchange())
                .with("alert.#");
    }
}
