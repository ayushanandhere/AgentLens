package dev.ayush.agentlens.policy.engine.evaluators;

import dev.ayush.agentlens.policy.engine.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Component
public class CostBudgetEvaluator implements PolicyEvaluator {

    @Override
    public PolicyType getType() {
        return PolicyType.COST_BUDGET;
    }

    @Override
    public Set<PolicyEvaluationStage> stages() {
        return Set.of(PolicyEvaluationStage.COMPLETION);
    }

    @Override
    public PolicyResult evaluate(Map<String, Object> config, TraceContext context) {
        BigDecimal estimatedCost = context.getTrace().getEstimatedCost();
        if (estimatedCost == null) {
            return PolicyResult.passed();
        }

        BigDecimal maxCost = new BigDecimal(config.get("max_cost_per_run_usd").toString());
        if (estimatedCost.compareTo(maxCost) > 0) {
            BigDecimal exceededBy = estimatedCost.subtract(maxCost);
            return PolicyResult.violated(Map.of(
                    "limit_usd", maxCost,
                    "actual_usd", estimatedCost,
                    "exceeded_by_usd", exceededBy
            ));
        }
        return PolicyResult.passed();
    }
}
