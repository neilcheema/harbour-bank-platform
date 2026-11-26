-- Week 2: first schema version. Flyway applies this file once, in order, and records it in
-- flyway_schema_history. Never edit a migration after it has been applied anywhere shared:
-- add a new V2__..., V3__... file instead.
--
-- Rules live in the database as well as in Java. Java checks give friendly errors; the
-- constraints below are the guarantee that holds even under concurrent requests and bugs.

CREATE TABLE customers (
    id          UUID         PRIMARY KEY,
    version     BIGINT       NOT NULL,                 -- optimistic locking (@Version)
    full_name   VARCHAR(120) NOT NULL,
    email       VARCHAR(254) NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_customers_email  UNIQUE (email),
    CONSTRAINT ck_customers_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

-- Source of account numbers ("HB-" plus ten digits). Sequences never hand out the same value
-- twice, even across concurrent transactions. A rolled-back transaction leaves a gap, which is
-- normal and harmless for account numbers.
CREATE SEQUENCE account_number_seq START WITH 1000000000 INCREMENT BY 1;

CREATE TABLE accounts (
    id              UUID        PRIMARY KEY,
    version         BIGINT      NOT NULL,              -- optimistic locking (@Version)
    customer_id     UUID        NOT NULL,
    account_number  VARCHAR(20) NOT NULL,
    type            VARCHAR(20) NOT NULL,
    currency        VARCHAR(3)  NOT NULL,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_accounts_customer       FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT uq_accounts_account_number UNIQUE (account_number),
    CONSTRAINT ck_accounts_type           CHECK (type IN ('CHEQUING', 'SAVINGS', 'TFSA')),
    CONSTRAINT ck_accounts_currency       CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_accounts_status         CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'))
);

-- Foreign keys are not indexed automatically in PostgreSQL. This index serves
-- "list this customer's accounts", and the foreign key's own checks.
CREATE INDEX ix_accounts_customer_id ON accounts (customer_id);
