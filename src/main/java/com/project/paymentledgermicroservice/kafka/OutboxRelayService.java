package com.project.paymentledgermicroservice.kafka;

import com.project.paymentledgermicroservice.entity.OutboxEvent;
import com.project.paymentledgermicroservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "5000")
    public void processPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent event : pendingEvents) {
            kafkaTemplate.send("payment-events", event.getAggregateId(), event.getPayload());
            event.setStatus("PUBLISHED");
        }

        if (!pendingEvents.isEmpty()) {
            outboxEventRepository.saveAll(pendingEvents);
        }
    }
}
