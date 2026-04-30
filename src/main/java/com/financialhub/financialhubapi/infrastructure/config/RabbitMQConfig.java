package com.financialhub.financialhubapi.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME    = "summary.notifications";
    public static final String EXCHANGE_NAME = "summary.exchange";
    public static final String ROUTING_KEY   = "summary.send";

    public static final String DLX_NAME  = "summary.dlx";
    public static final String DLQ_NAME  = "summary.notifications.dlq";

    // Main queue — failed/rejected messages are routed to the DLX
    @Bean
    public Queue summaryQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange summaryExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding summaryBinding(Queue summaryQueue, DirectExchange summaryExchange) {
        return BindingBuilder.bind(summaryQueue).to(summaryExchange).with(ROUTING_KEY);
    }

    // Dead-letter infrastructure
    @Bean
    public DirectExchange summaryDlx() {
        return new DirectExchange(DLX_NAME);
    }

    @Bean
    public Queue summaryDlq() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public Binding summaryDlqBinding(Queue summaryDlq, DirectExchange summaryDlx) {
        return BindingBuilder.bind(summaryDlq).to(summaryDlx).with(ROUTING_KEY);
    }

    @Bean
    public ApplicationRunner rabbitEagerConnect(CachingConnectionFactory connectionFactory) {
        return args -> connectionFactory.createConnection().close();
    }

}
