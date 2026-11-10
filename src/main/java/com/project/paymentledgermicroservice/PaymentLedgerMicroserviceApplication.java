package com.project.paymentledgermicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaymentLedgerMicroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentLedgerMicroserviceApplication.class, args);
    }

}
