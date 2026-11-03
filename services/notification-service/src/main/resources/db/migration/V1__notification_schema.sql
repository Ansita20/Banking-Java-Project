create table recipient (
    user_id   varchar(36)  primary key,
    email     varchar(255) not null,
    full_name varchar(255) not null
);

create table notification (
    id           uuid          primary key,
    user_id      varchar(36)   not null,
    category     varchar(30)   not null,
    title        varchar(255)  not null,
    message      varchar(1000) not null,
    source_event varchar(255)  not null unique,
    created_at   timestamp with time zone not null,
    read_at      timestamp with time zone
);
create index idx_notification_user_time on notification (user_id, created_at desc);
