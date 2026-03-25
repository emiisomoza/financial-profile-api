package com.finantialhub.finantialhubapi.infrastructure.messaging;

import tools.jackson.databind.ObjectMapper;
import com.finantialhub.finantialhubapi.application.usecases.SummaryService;
import com.finantialhub.finantialhubapi.domain.model.User;
import com.finantialhub.finantialhubapi.domain.ports.SummaryPublisherPort;
import com.finantialhub.finantialhubapi.infrastructure.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SummaryPublisher implements SummaryPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(SummaryPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public SummaryPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(SummaryService.Summary summary, User user) {
        try {
            SummaryMessage payload = new SummaryMessage(
                    user.getId().toString(),
                    user.getEmail(),
                    user.getFullName(),
                    summary.totalAssetsValue(),
                    summary.monthlyIncome(),
                    summary.monthlyExpenses(),
                    summary.monthlySavings(),
                    summary.savingsRate(),
                    summary.unpricedAssetsCount(),
                    summary.currency(),
                    Instant.now().toString()
            );

            byte[] body = objectMapper.writeValueAsBytes(payload);
            Message message = MessageBuilder.withBody(body)
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build();

            rabbitTemplate.send(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, message);

        } catch (Exception e) {
            log.error("Failed to publish summary for user {}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Could not publish summary to message queue", e);
        }
    }

    public record SummaryMessage(
            String userId,
            String userEmail,
            String userName,
            java.math.BigDecimal totalAssetsValue,
            java.math.BigDecimal monthlyIncome,
            java.math.BigDecimal monthlyExpenses,
            java.math.BigDecimal monthlySavings,
            java.math.BigDecimal savingsRate,
            int unpricedAssetsCount,
            String currency,
            String generatedAt
    ) {}
}
