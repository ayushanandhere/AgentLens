CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    timestamp       TIMESTAMP DEFAULT NOW(),
    actor           VARCHAR(255) NOT NULL,
    action          VARCHAR(50) NOT NULL,
    resource_type   VARCHAR(30),
    resource_id     VARCHAR(100),
    details         JSONB,
    ip_address      VARCHAR(45)
);

CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_log_actor ON audit_log(actor);
CREATE INDEX idx_audit_log_action ON audit_log(action);
