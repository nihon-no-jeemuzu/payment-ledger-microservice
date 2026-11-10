package com.project.paymentledgermicroservice.controller;

import com.project.paymentledgermicroservice.dto.PaymentRequest;
import com.project.paymentledgermicroservice.dto.PaymentResponse;
import com.project.paymentledgermicroservice.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final LedgerService ledgerService;

    @PostMapping("/transfer")
    public ResponseEntity<PaymentResponse> transfer(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestBody PaymentRequest request) {
        ledgerService.executeTransfer(request.getSourceAccountId(), request.getTargetAccountId(), request.getAmount());
        return ResponseEntity.accepted()
                .body(new PaymentResponse("Transfer initiated", "PROCESSING"));
    }
}
