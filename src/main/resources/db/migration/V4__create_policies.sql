CREATE TABLE policies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    policy_type     VARCHAR(30) NOT NULL,
    config          JSONB NOT NULL,
    scope           VARCHAR(20) DEFAULT 'GLOBAL',
    scope_id        VARCHAR(100),
    enabled         BOOLEAN DEFAULT TRUE,
    severity        VARCHAR(10) DEFAULT 'BLOCK',
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);
