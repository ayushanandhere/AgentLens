CREATE TABLE policy_violations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trace_id        UUID NOT NULL REFERENCES traces(id),
    policy_id       UUID NOT NULL REFERENCES policies(id),
    violation_type  VARCHAR(30) NOT NULL,
    severity        VARCHAR(10) NOT NULL,
    details         JSONB,
    action_taken    VARCHAR(20),
    resolved_by     VARCHAR(255),
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_violations_trace_id ON policy_violations(trace_id);
CREATE INDEX idx_violations_policy_id ON policy_violations(policy_id);
