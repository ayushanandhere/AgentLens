package dev.ayush.agentlens.policy.engine;

import java.util.Set;
import java.util.Map;

public interface PolicyEvaluator {
    PolicyType getType();
    Set<PolicyEvaluationStage> stages();
    PolicyResult evaluate(Map<String, Object> config, TraceContext context);
}
