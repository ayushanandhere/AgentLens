package dev.ayush.agentlens.policy.engine;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PolicyEvaluatorRegistry {

    private final List<PolicyEvaluator> evaluators;
    private final Map<PolicyType, PolicyEvaluator> registry = new EnumMap<>(PolicyType.class);

    public PolicyEvaluatorRegistry(List<PolicyEvaluator> evaluators) {
        this.evaluators = evaluators;
    }

    @PostConstruct
    void init() {
        for (PolicyEvaluator evaluator : evaluators) {
            registry.put(evaluator.getType(), evaluator);
        }
    }

    public PolicyEvaluator getEvaluator(PolicyType type) {
        PolicyEvaluator evaluator = registry.get(type);
        if (evaluator == null) {
            throw new IllegalArgumentException("No evaluator registered for policy type: " + type);
        }
        return evaluator;
    }
}
