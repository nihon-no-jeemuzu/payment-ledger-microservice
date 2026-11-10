package com.project.paymentledgermicroservice.service;

import tools.jackson.databind.json.JsonMapper;
import com.project.paymentledgermicroservice.entity.Account;
import com.project.paymentledgermicroservice.entity.LedgerEntry;
import com.project.paymentledgermicroservice.entity.OutboxEvent;
import com.project.paymentledgermicroservice.entity.PaymentTransaction;
import com.project.paymentledgermicroservice.repository.AccountRepository;
import com.project.paymentledgermicroservice.repository.LedgerEntryRepository;
import com.project.paymentledgermicroservice.repository.OutboxEventRepository;
import com.project.paymentledgermicroservice.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final AccountRepository accountRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    @Transactional
    public void executeTransfer(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount) {
        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));
        Account targetAccount = accountRepository.findById(targetAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Target account not found"));

        PaymentTransaction transaction = PaymentTransaction.builder()
                .sourceAccountId(sourceAccount)
                .targetAccountId(targetAccount)
                .amount(amount)
                .currency(sourceAccount.getCurrency())
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .build();

        // Save to generate the ID needed for the OutboxEvent
        transaction = paymentTransactionRepository.save(transaction);

        LedgerEntry debitEntry = LedgerEntry.builder()
                .transaction(transaction)
                .account(sourceAccount)
                .entryType("DEBIT")
                .amount(amount)
                .balanceAfter(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .transaction(transaction)
                .account(targetAccount)
                .entryType("CREDIT")
                .amount(amount)
                .balanceAfter(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .build();

        if (debitEntry.getAmount().compareTo(creditEntry.getAmount()) != 0) {
            throw new IllegalStateException("Debit and credit amounts must match for zero-sum constraint");
        }

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        // Safely serialize the JSON payload
        String payloadJson;
        try {
            payloadJson = jsonMapper.writeValueAsString(Map.of(
                    "transactionId", transaction.getId().toString(),
                    "amount", amount
            ));
        } catch (Exception e) {
            log.error("Failed to serialize outbox payload for transaction: {}", transaction.getId(), e);
            throw new IllegalStateException("Could not serialize outbox payload", e);
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(UUID.randomUUID()) // Explicitly generate UUID for the outbox event
                .aggregateType("PAYMENT")
                .aggregateId(transaction.getId().toString())
                .eventType("PAYMENT_COMPLETED")
                .payload(payloadJson)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        outboxEventRepository.save(outboxEvent);
    }
}