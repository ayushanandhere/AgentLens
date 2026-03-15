package dev.ayush.agentlens.policy.engine;

import java.util.Map;

public interface PolicyEvaluator {
    PolicyType getType();
    PolicyResult evaluate(Map<String, Object> config, TraceContext context);
}
