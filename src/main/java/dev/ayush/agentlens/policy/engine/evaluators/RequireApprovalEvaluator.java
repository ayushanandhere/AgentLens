package dev.ayush.agentlens.policy.engine.evaluators;

import dev.ayush.agentlens.policy.engine.PolicyEvaluationStage;
import dev.ayush.agentlens.policy.engine.PolicyEvaluator;
import dev.ayush.agentlens.policy.engine.PolicyResult;
import dev.ayush.agentlens.policy.engine.PolicyType;
import dev.ayush.agentlens.policy.engine.TraceContext;
import dev.ayush.agentlens.trace.TraceEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RequireApprovalEvaluator implements PolicyEvaluator {

    @Override
    public PolicyType getType() {
        return PolicyType.REQUIRE_APPROVAL;
    }

    @Override
    public Set<PolicyEvaluationStage> stages() {
        return Set.of(PolicyEvaluationStage.EVENT_INGEST);
    }

    @Override
    @SuppressWarnings("unchecked")
    public PolicyResult evaluate(Map<String, Object> config, TraceContext context) {
        TraceEvent event = context.getCurrentEvent();
        if (event == null || !"TOOL_CALL".equals(event.getEventType())) {
            return PolicyResult.passed();
        }

        List<String> tools = (List<String>) config.get("for_tools");
        if (tools == null || tools.isEmpty() || !tools.contains(event.getEventName())) {
            return PolicyResult.passed();
        }

        return PolicyResult.violated(Map.of(
                "approval_required", true,
                "tool", event.getEventName(),
                "allowed_approvers", config.getOrDefault("approvers", List.of())
        ));
    }
}
