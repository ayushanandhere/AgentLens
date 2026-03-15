CREATE TABLE trace_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trace_id        UUID NOT NULL REFERENCES traces(id),
    event_type      VARCHAR(30) NOT NULL,
    event_name      VARCHAR(255),
    input_data      JSONB,
    output_data     JSONB,
    status          VARCHAR(20),
    duration_ms     BIGINT,
    error_message   TEXT,
    sequence_num    INTEGER,
    timestamp       TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_trace_events_trace_id ON trace_events(trace_id);
CREATE INDEX idx_trace_events_event_type ON trace_events(event_type);
