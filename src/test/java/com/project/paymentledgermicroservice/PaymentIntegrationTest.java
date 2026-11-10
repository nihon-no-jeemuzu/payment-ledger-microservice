package com.project.paymentledgermicroservice;

import com.project.paymentledgermicroservice.dto.PaymentRequest;
import com.project.paymentledgermicroservice.dto.PaymentResponse;
import com.project.paymentledgermicroservice.entity.Account;
import com.project.paymentledgermicroservice.entity.OutboxEvent;
import com.project.paymentledgermicroservice.entity.ProcessedEvent;
import com.project.paymentledgermicroservice.repository.AccountRepository;
import com.project.paymentledgermicroservice.repository.OutboxEventRepository;
import com.project.paymentledgermicroservice.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    );

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void shouldProcessPaymentAndDispatchEvent() throws InterruptedException {
        // Setup: Create source and target accounts
        Account sourceAccount = Account.builder()
                .userId("user-source")
                .currency("USD")
                .createdAt(LocalDateTime.now())
                .build();
        sourceAccount = accountRepository.save(sourceAccount);

        Account targetAccount = Account.builder()
                .userId("user-target")
                .currency("USD")
                .createdAt(LocalDateTime.now())
                .build();
        targetAccount = accountRepository.save(targetAccount);

        // Action: Send payment request
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Idempotency-Key", UUID.randomUUID().toString());

        PaymentRequest request = new PaymentRequest();
        request.setSourceAccountId(sourceAccount.getId());
        request.setTargetAccountId(targetAccount.getId());
        request.setAmount(new BigDecimal("100.00"));

        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(
                "/api/v1/payments/transfer",
                entity,
                PaymentResponse.class
        );

        // Assert: HTTP response
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PROCESSING", response.getBody().getStatus());

        // Async Assert: Wait for outbox poller and Kafka consumer
        Thread.sleep(6000);

        // Final Assert: Verify OutboxEvent status and ProcessedEvent
        OutboxEvent outboxEvent = outboxEventRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("No outbox event found"));

        assertEquals("PUBLISHED", outboxEvent.getStatus());

        ProcessedEvent processedEvent = processedEventRepository.findById(outboxEvent.getAggregateId())
                .orElseThrow(() -> new AssertionError("No processed event found"));

        assertNotNull(processedEvent);
    }
}
