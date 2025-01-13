CREATE SCHEMA IF NOT EXISTS bankdemo;
--DROP TABLE IF EXISTS bankdemo.bills;
CREATE TABLE IF NOT EXISTS bankdemo.bills(
	id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	created_at TIMESTAMP NOT NULL,
	updated_at TIMESTAMP,
	is_active BOOLEAN NOT NULL,
	balance NUMERIC(19,2) NOT NULL,
	currency VARCHAR(3) NOT NULL CHECK (CHAR_LENGTH(currency) = 3),
	owner INTEGER NOT NULL
);