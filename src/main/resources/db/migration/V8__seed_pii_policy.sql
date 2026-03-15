INSERT INTO policies (id, name, description, policy_type, config, scope, enabled, severity)
SELECT
    gen_random_uuid(),
    'PII Detection (Global)',
    'Warn when prompts or responses contain common PII patterns',
    'PII_CHECK',
    '{"patterns": ["email", "phone", "ssn", "credit_card"], "action": "WARN"}',
    'GLOBAL',
    true,
    'WARN'
WHERE NOT EXISTS (
    SELECT 1
    FROM policies
    WHERE name = 'PII Detection (Global)'
);
