CREATE TABLE insight_snapshots (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id),
    "month"      VARCHAR(7)   NOT NULL,
    type         VARCHAR(20)  NOT NULL
        CHECK (type IN ('STORY', 'MOOD', 'PREDICTION', 'GAMIFICATION')),
    payload      TEXT         NOT NULL,
    generated_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, "month", type)
);

