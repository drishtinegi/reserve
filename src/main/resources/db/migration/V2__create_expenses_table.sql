CREATE TABLE expenses (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT         NOT NULL REFERENCES users(id),
    amount         NUMERIC(12, 2) NOT NULL,
    category       VARCHAR(100)   NOT NULL,
    description    VARCHAR(500),
    date           DATE           NOT NULL,
    mood           VARCHAR(20)    NOT NULL
        CHECK (mood IN ('HAPPY', 'STRESSED', 'NEUTRAL', 'SAD', 'EXCITED')),
    currency       VARCHAR(10)    NOT NULL
        CHECK (currency IN ('USD', 'INR')),
    payment_method VARCHAR(100),
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);
