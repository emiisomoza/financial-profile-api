package com.finantialhub.finantialhubapi.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME    = "summary.notifications";
    public static final String EXCHANGE_NAME = "summary.exchange";
    public static final String ROUTING_KEY   = "summary.send";

    @Bean
    public Queue summaryQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public DirectExchange summaryExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding summaryBinding(Queue summaryQueue, DirectExchange summaryExchange) {
        return BindingBuilder.bind(summaryQueue).to(summaryExchange).with(ROUTING_KEY);
    }

}
