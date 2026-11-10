System Architecture & Domain Rules

Domain: FinTech Payment & Ledger Engine.

Architecture: Event-driven microservice using the Transactional Outbox pattern.

Tech Stack: Java 21, Spring Boot 3, PostgreSQL 16, Apache Kafka (KRaft), Redis (Redisson).

Strict Ledger Rule (Double-Entry Bookkeeping): Never mutate account balances via SQL UPDATE. Account states are calculated by summing immutable, append-only CREDIT and DEBIT entries in the ledger table. The net delta of any transaction must always equal zero.

Concurrency Rule: All payment executions must be guarded by Redis distributed locks to prevent double-spending during network retries.