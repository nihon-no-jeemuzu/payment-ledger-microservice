CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    currency VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR UNIQUE NOT NULL,
    source_account_id UUID NOT NULL,
    target_account_id UUID NOT NULL,
    amount NUMERIC(18,4) NOT NULL,
    currency VARCHAR NOT NULL,
    status VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_source_account FOREIGN KEY (source_account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_target_account FOREIGN KEY (target_account_id) REFERENCES accounts(id) ON DELETE RESTRICT
);

CREATE TABLE ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    transaction_id UUID NOT NULL,
    account_id UUID NOT NULL,
    entry_type VARCHAR NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount NUMERIC(18,4) NOT NULL,
    balance_after NUMERIC(18,4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction FOREIGN KEY (transaction_id) REFERENCES payment_transactions(id) ON DELETE RESTRICT,
    CONSTRAINT fk_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT
);
