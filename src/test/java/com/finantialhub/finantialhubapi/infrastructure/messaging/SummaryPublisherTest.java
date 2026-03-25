package com.finantialhub.finantialhubapi.infrastructure.messaging;

import tools.jackson.databind.ObjectMapper;
import com.finantialhub.finantialhubapi.application.usecases.SummaryService;
import com.finantialhub.finantialhubapi.domain.model.Role;
import com.finantialhub.finantialhubapi.domain.model.User;
import com.finantialhub.finantialhubapi.infrastructure.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private SummaryPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SummaryPublisher(rabbitTemplate, new ObjectMapper());
    }

    @Test
    void publish_sendsJsonMessageToCorrectExchangeAndRoutingKey() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "alice@example.com", "Alice", "hash", Instant.now(), Role.MEMBER);
        SummaryService.Summary summary = summary(userId);

        publisher.publish(summary, user);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY),
                messageCaptor.capture()
        );

        Message sent = messageCaptor.getValue();
        assertThat(sent.getMessageProperties().getContentType())
                .isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
    }

    @Test
    void publish_messageBodyContainsUserAndSummaryFields() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "alice@example.com", "Alice", "hash", Instant.now(), Role.MEMBER);
        SummaryService.Summary summary = summary(userId);

        publisher.publish(summary, user);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(anyString(), anyString(), messageCaptor.capture());

        String body = new String(messageCaptor.getValue().getBody());
        assertThat(body).contains("alice@example.com");
        assertThat(body).contains("Alice");
        assertThat(body).contains("AUD");
        assertThat(body).contains(userId.toString());
        assertThat(body).contains("5000");
        assertThat(body).contains("3000");
    }

    @Test
    void publish_whenRabbitTemplateFails_throwsRuntimeException() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "alice@example.com", "Alice", "hash", Instant.now(), Role.MEMBER);
        SummaryService.Summary summary = summary(userId);

        doThrow(new RuntimeException("Broker unavailable"))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        assertThrows(RuntimeException.class, () -> publisher.publish(summary, user));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static SummaryService.Summary summary(UUID userId) {
        return new SummaryService.Summary(
                userId, "AUD",
                new BigDecimal("100000"),
                new BigDecimal("5000"),
                new BigDecimal("3000"),
                new BigDecimal("2000"),
                new BigDecimal("0.40"),
                0
        );
    }
}
