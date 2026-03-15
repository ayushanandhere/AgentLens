CREATE TABLE traces (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id        UUID NOT NULL REFERENCES agents(id),
    trace_id        VARCHAR(64),
    status          VARCHAR(20) NOT NULL,
    model           VARCHAR(100),
    prompt_text     TEXT,
    response_text   TEXT,
    temperature     DECIMAL(3,2),
    input_tokens    INTEGER,
    output_tokens   INTEGER,
    total_tokens    INTEGER,
    estimated_cost  DECIMAL(10,6),
    started_at      TIMESTAMP NOT NULL,
    completed_at    TIMESTAMP,
    latency_ms      BIGINT,
    ttft_ms         BIGINT,
    grounding_score DECIMAL(3,2),
    policy_result   VARCHAR(20),
    blocked_reason  TEXT,
    tenant_id       VARCHAR(100),
    session_id      VARCHAR(100),
    metadata        JSONB,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_traces_agent_id ON traces(agent_id);
CREATE INDEX idx_traces_status ON traces(status);
CREATE INDEX idx_traces_started_at ON traces(started_at);
CREATE INDEX idx_traces_tenant_id ON traces(tenant_id);
CREATE INDEX idx_traces_policy_result ON traces(policy_result);
