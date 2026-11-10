package com.project.paymentledgermicroservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentRequest {
    private UUID sourceAccountId;
    private UUID targetAccountId;
    private BigDecimal amount;
}
