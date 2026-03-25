CREATE TABLE IF NOT EXISTS users (
    id               UUID PRIMARY KEY,
    email            VARCHAR(255) NOT NULL UNIQUE,
    full_name        VARCHAR(255) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    role          VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS assets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    symbol VARCHAR(50),
    quantity NUMERIC(18, 8) NOT NULL,
    valuation_mode VARCHAR(20) NOT NULL,
    manual_unit_value NUMERIC(18, 8),
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_assets_user
      FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS incomes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    source VARCHAR(255) NOT NULL,
    frequency VARCHAR(30) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    starts_at DATE NOT NULL,
    ends_at DATE,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_incomes_user
       FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS expenses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    frequency VARCHAR(30) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    starts_at DATE NOT NULL,
    ends_at DATE,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_expenses_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS summary_subscriptions (
    id               UUID PRIMARY KEY,
    user_id          UUID NOT NULL,
    frequency        VARCHAR(20) NOT NULL,
    currency         VARCHAR(3) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    next_send_at     TIMESTAMP NOT NULL,
    created_at       TIMESTAMP NOT NULL,

    CONSTRAINT fk_summary_subscriptions_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);