CREATE TABLE users (
    id                 BIGSERIAL PRIMARY KEY,
    username           VARCHAR(100) NOT NULL UNIQUE,
    password           VARCHAR(255) NOT NULL,
    email              VARCHAR(255) NOT NULL,
    preferred_currency VARCHAR(10)  NOT NULL
        CHECK (preferred_currency IN ('USD', 'INR'))
);
