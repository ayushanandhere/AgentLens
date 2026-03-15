package dev.ayush.agentlens.policy.engine.evaluators;

import dev.ayush.agentlens.policy.engine.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TokenBudgetEvaluator implements PolicyEvaluator {

    @Override
    public PolicyType getType() {
        return PolicyType.TOKEN_BUDGET;
    }

    @Override
    public PolicyResult evaluate(Map<String, Object> config, TraceContext context) {
        Integer totalTokens = context.getTrace().getTotalTokens();
        if (totalTokens == null) {
            return PolicyResult.passed();
        }

        int maxTokens = ((Number) config.get("max_tokens_per_run")).intValue();
        if (totalTokens > maxTokens) {
            return PolicyResult.violated(Map.of(
                    "limit", maxTokens,
                    "actual", totalTokens,
                    "exceeded_by", totalTokens - maxTokens
            ));
        }
        return PolicyResult.passed();
    }
}
