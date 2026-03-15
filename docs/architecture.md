# AgentLens Architecture

## Runtime components

- Spring Boot API: owns ingestion, policy evaluation, analytics, and operator-facing endpoints.
- PostgreSQL: stores agents, traces, events, policies, violations, and audit records.
- Redis: backs rate-limit counters.
- Kafka: carries trace completion, policy violation, and audit events.
- Jaeger via OTLP: receives backend spans from the OpenTelemetry starter.
- React dashboard: operator UI that polls the REST API every 5 seconds.

## Policy evaluation stages

### 1. Pre-execution

- Runs when a trace starts.
- Current use: `RATE_LIMIT`.
- Result can immediately block a trace before any work continues.

### 2. Event ingest

- Runs on every `POST /events`.
- Current use: `TOOL_BLOCK`, `PII_CHECK`, `REQUIRE_APPROVAL`.
- This is the main governance upgrade in vNext because risky actions are intercepted as events arrive instead of only at completion time.

### 3. Completion

- Runs on `PUT /complete`.
- Current use: `TOKEN_BUDGET`, `COST_BUDGET`, `TOOL_BLOCK`, `PII_CHECK`.
- Produces the final stored verdict for completed traces unless a stronger earlier verdict already exists.

## Approval state machine

```text
RUNNING
  ├─ event matches REQUIRE_APPROVAL ─> PENDING_APPROVAL
  │                                     ├─ approve ─> RUNNING
  │                                     └─ reject  ─> BLOCKED
  ├─ event/completion BLOCK policy ─> BLOCKED
  └─ complete successfully ─> COMPLETED
```

## Security model

- All non-actuator endpoints require `X-API-Key`.
- `INGEST` keys can create traces, add events, and complete traces.
- `OPERATOR` keys can read traces, analytics, audit data, and manage policies and violations.

## Observability model

- Custom spans wrap trace start, event ingest, trace completion, policy evaluation, and Redis-backed rate-limit checks.
- Micrometer counters emit trace starts, completions, blocks, pending approvals, and policy violations by type/stage/action.
- Kafka publish/consume and HTTP spans are auto-instrumented by the OpenTelemetry starter.
