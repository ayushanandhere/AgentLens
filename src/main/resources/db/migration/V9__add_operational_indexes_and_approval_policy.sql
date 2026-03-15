CREATE INDEX IF NOT EXISTS idx_policies_enabled_type_scope
    ON policies(enabled, policy_type, scope);

CREATE INDEX IF NOT EXISTS idx_policy_violations_action_taken
    ON policy_violations(action_taken);

CREATE INDEX IF NOT EXISTS idx_policy_violations_severity_created_at
    ON policy_violations(severity, created_at DESC);

INSERT INTO policies (id, name, description, policy_type, config, scope, enabled, severity)
SELECT
    gen_random_uuid(),
    'Production Tool Approval',
    'Require human approval before running sensitive production tools',
    'REQUIRE_APPROVAL',
    '{"for_tools": ["execShell", "sendNotification"], "approvers": ["admin@agentlens.dev"]}',
    'GLOBAL',
    true,
    'BLOCK'
WHERE NOT EXISTS (
    SELECT 1
    FROM policies
    WHERE name = 'Production Tool Approval'
);
