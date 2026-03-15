INSERT INTO policies (id, name, description, policy_type, config, scope, enabled, severity)
VALUES
(gen_random_uuid(), 'Token Budget (Global)', 'Warn when a single agent run exceeds 8000 tokens',
 'TOKEN_BUDGET', '{"max_tokens_per_run": 8000}', 'GLOBAL', true, 'WARN'),

(gen_random_uuid(), 'Cost Budget (Global)', 'Block agent runs that would cost more than $1.00',
 'COST_BUDGET', '{"max_cost_per_run_usd": 1.00}', 'GLOBAL', true, 'BLOCK'),

(gen_random_uuid(), 'Dangerous Tools Blocklist', 'Block agents from calling destructive tools',
 'TOOL_BLOCK', '{"blocked_tools": ["deleteDatabase", "dropTable", "execShell", "rmrf"]}', 'GLOBAL', true, 'BLOCK'),

(gen_random_uuid(), 'Global Rate Limit', 'Limit agents to 30 runs per minute',
 'RATE_LIMIT', '{"max_runs_per_minute": 30}', 'GLOBAL', true, 'BLOCK');
