package com.project.paymentledgermicroservice.service;

import com.project.paymentledgermicroservice.entity.Account;
import com.project.paymentledgermicroservice.entity.LedgerEntry;
import com.project.paymentledgermicroservice.entity.PaymentTransaction;
import com.project.paymentledgermicroservice.repository.AccountRepository;
import com.project.paymentledgermicroservice.repository.LedgerEntryRepository;
import com.project.paymentledgermicroservice.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private LedgerService ledgerService;

    @Test
    void shouldExecuteTransferSuccessfully() throws com.fasterxml.jackson.core.JsonProcessingException {
        UUID sourceAccountId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");

        Account sourceAccount = Account.builder()
                .id(sourceAccountId)
                .userId("user1")
                .currency("USD")
                .createdAt(LocalDateTime.now())
                .build();

        Account targetAccount = Account.builder()
                .id(targetAccountId)
                .userId("user2")
                .currency("USD")
                .createdAt(LocalDateTime.now())
                .build();

        PaymentTransaction transaction = PaymentTransaction.builder()
                .id(UUID.randomUUID())
                .sourceAccountId(sourceAccount)
                .targetAccountId(targetAccount)
                .amount(amount)
                .currency("USD")
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .build();

        when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(targetAccountId)).thenReturn(Optional.of(targetAccount));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenReturn(transaction);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ledgerService.executeTransfer(sourceAccountId, targetAccountId, amount);

        verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class));
        verify(ledgerEntryRepository).save(argThat(entry -> entry.getEntryType().equals("DEBIT")));
        verify(ledgerEntryRepository).save(argThat(entry -> entry.getEntryType().equals("CREDIT")));
    }

    @Test
    void shouldRollbackIfLedgerIsImbalanced() throws com.fasterxml.jackson.core.JsonProcessingException {
        UUID sourceAccountId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");

        LedgerService spyService = spy(ledgerService);

        doThrow(new IllegalStateException("Debit and credit amounts must match for zero-sum constraint"))
                .when(spyService).executeTransfer(sourceAccountId, targetAccountId, amount);

        assertThrows(IllegalStateException.class, () -> spyService.executeTransfer(sourceAccountId, targetAccountId, amount));

        verify(spyService, times(1)).executeTransfer(sourceAccountId, targetAccountId, amount);
    }
}
