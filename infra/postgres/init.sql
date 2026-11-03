-- One database per service: no service can read another's tables.
CREATE DATABASE identity_db;
CREATE DATABASE account_db;
CREATE DATABASE transaction_db;
CREATE DATABASE notification_db;
