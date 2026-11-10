package com.project.paymentledgermicroservice.kafka;

import com.project.paymentledgermicroservice.entity.ProcessedEvent;
import com.project.paymentledgermicroservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final ProcessedEventRepository processedEventRepository;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "payment-events", groupId = "payment-group")
    public void consumePaymentEvent(String payload) {
        try {
            String transactionId = jsonMapper.readTree(payload).get("transactionId").asString();

            if (processedEventRepository.existsById(transactionId)) {
                log.info("Duplicate event safely skipped for transactionId: {}", transactionId);
                return;
            }

            ProcessedEvent processedEvent = ProcessedEvent.builder()
                    .eventId(transactionId)
                    .processedAt(LocalDateTime.now())
                    .build();
            processedEventRepository.save(processedEvent);

            log.info("Successfully processed event for transactionId: {}", transactionId);
        } catch (Exception e) {
            log.error("Error processing payment event with payload: {}", payload, e);
            throw e;
        }
    }
}
