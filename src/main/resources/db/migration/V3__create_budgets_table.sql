CREATE TABLE budgets (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT         NOT NULL REFERENCES users(id),
    category      VARCHAR(100)   NOT NULL,
    monthly_limit NUMERIC(12, 2) NOT NULL,
    currency      VARCHAR(10)    NOT NULL
        CHECK (currency IN ('USD', 'INR')),
    UNIQUE (user_id, category)
);
