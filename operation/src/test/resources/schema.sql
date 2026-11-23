CREATE SCHEMA IF NOT EXISTS bankdemo;
CREATE TABLE IF NOT EXISTS bankdemo.operations(
	id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	created_at TIMESTAMP NOT NULL,
	amount DOUBLE PRECISION NOT NULL,
	action VARCHAR(10) NOT NULL,
	currency VARCHAR(3) NOT NULL CHECK (CHAR_LENGTH(currency) = 3),
	sender INTEGER,
	recipient INTEGER,
	bank VARCHAR(30) NOT NULL
);
CREATE TABLE IF NOT EXISTS bankdemo.operations_outbox (
    id              UUID PRIMARY KEY,
    payload         TEXT NOT NULL,
    status          VARCHAR(32) NOT NULL,
    attempts        INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_operations_outbox_poll ON bankdemo.operations_outbox (status, next_attempt_at);