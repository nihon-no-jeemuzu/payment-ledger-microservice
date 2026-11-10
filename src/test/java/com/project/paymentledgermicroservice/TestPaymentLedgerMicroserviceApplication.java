package com.project.paymentledgermicroservice;

import org.springframework.boot.SpringApplication;

public class TestPaymentLedgerMicroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.from(PaymentLedgerMicroserviceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
