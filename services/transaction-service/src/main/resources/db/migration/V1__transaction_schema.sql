create table transfer (
    id                  uuid           primary key,
    initiator_user_id   varchar(36)    not null,
    from_account        varchar(12)    not null,
    to_account          varchar(12)    not null,
    amount              numeric(19, 2) not null check (amount > 0),
    description         varchar(255),
    status              varchar(20)    not null,
    failure_reason      varchar(255),
    idempotency_key     varchar(100)   not null,
    request_fingerprint varchar(64)    not null,
    created_at          timestamp with time zone not null,
    updated_at          timestamp with time zone not null,
    version             bigint         not null default 0,
    constraint uq_transfer_idempotency unique (initiator_user_id, idempotency_key)
);
create index idx_transfer_user_time on transfer (initiator_user_id, created_at desc);
create index idx_transfer_status_updated on transfer (status, updated_at);

create table daily_usage (
    user_id    varchar(36)    not null,
    usage_date date           not null,
    used       numeric(19, 2) not null,
    primary key (user_id, usage_date)
);

create table beneficiary (
    id             uuid        primary key,
    user_id        varchar(36) not null,
    nickname       varchar(60) not null,
    account_number varchar(12) not null,
    holder_name    varchar(255),
    created_at     timestamp with time zone not null,
    constraint uq_beneficiary unique (user_id, account_number)
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
