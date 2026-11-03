create table customer (
    user_id        varchar(36)  primary key,
    email          varchar(255) not null,
    full_name      varchar(255) not null,
    email_verified boolean      not null,
    kyc_completed  boolean      not null,
    enabled        boolean      not null,
    updated_at     timestamp with time zone not null
);

create table account (
    account_number varchar(12)    primary key,
    user_id        varchar(36)    not null,
    type           varchar(20)    not null,
    status         varchar(20)    not null,
    balance        numeric(19, 2) not null check (balance >= 0),
    currency       varchar(3)     not null,
    status_reason  varchar(255),
    opened_at      timestamp with time zone not null,
    activated_at   timestamp with time zone,
    version        bigint         not null default 0
);
create index idx_account_user on account (user_id);
create index idx_account_status on account (status);

create table ledger_entry (
    id             uuid           primary key,
    account_number varchar(12)    not null references account (account_number),
    entry_type     varchar(30)    not null,
    direction      varchar(10)    not null,
    amount         numeric(19, 2) not null check (amount > 0),
    balance_after  numeric(19, 2) not null,
    reference      varchar(100)   not null,
    counterparty   varchar(64),
    description    varchar(255),
    created_at     timestamp with time zone not null,
    constraint uq_ledger_reference unique (reference, entry_type)
);
create index idx_ledger_account_time on ledger_entry (account_number, created_at desc);

create table processed_command (
    transfer_id   uuid        not null,
    command_type  varchar(30) not null,
    reply_payload text        not null,
    processed_at  timestamp with time zone not null,
    primary key (transfer_id, command_type)
);

create table outbox_event (
    id           uuid         primary key,
    topic        varchar(100) not null,
    message_key  varchar(100) not null,
    event_type   varchar(100) not null,
    payload      text         not null,
    created_at   timestamp with time zone not null,
    published_at timestamp with time zone
);
create index idx_outbox_unpublished on outbox_event (created_at) where published_at is null;
