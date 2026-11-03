create table users (
    id                    varchar(36)  primary key,
    email                 varchar(255) not null unique,
    password_hash         varchar(100) not null,
    first_name            varchar(100) not null,
    last_name             varchar(100) not null,
    role                  varchar(20)  not null,
    email_verified        boolean      not null default false,
    enabled               boolean      not null default true,
    failed_login_attempts int          not null default 0,
    locked_until          timestamp with time zone,
    phone                 varchar(20),
    date_of_birth         date,
    gender                varchar(10),
    address_line          varchar(255),
    city                  varchar(100),
    state                 varchar(100),
    postal_code           varchar(10),
    pan_number            varchar(255),
    government_id         varchar(255),
    kyc_completed_at      timestamp with time zone,
    created_at            timestamp with time zone not null,
    version               bigint       not null default 0
);

create table one_time_code (
    id          uuid        primary key,
    user_id     varchar(36) not null references users (id),
    purpose     varchar(30) not null,
    code_hash   varchar(64) not null,
    created_at  timestamp with time zone not null,
    expires_at  timestamp with time zone not null,
    attempts    int         not null default 0,
    consumed_at timestamp with time zone
);
create index idx_otp_user_purpose on one_time_code (user_id, purpose, created_at desc);

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
