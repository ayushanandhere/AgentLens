package dev.ayush.agentlens.policy.engine.evaluators;

import dev.ayush.agentlens.policy.engine.*;
import dev.ayush.agentlens.trace.TraceEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ToolBlocklistEvaluator implements PolicyEvaluator {

    @Override
    public PolicyType getType() {
        return PolicyType.TOOL_BLOCK;
    }

    @Override
    public Set<PolicyEvaluationStage> stages() {
        return Set.of(PolicyEvaluationStage.EVENT_INGEST, PolicyEvaluationStage.COMPLETION);
    }

    @Override
    @SuppressWarnings("unchecked")
    public PolicyResult evaluate(Map<String, Object> config, TraceContext context) {
        List<String> blockedTools = (List<String>) config.get("blocked_tools");
        if (blockedTools == null || blockedTools.isEmpty()) {
            return PolicyResult.passed();
        }

        if (context.getStage() == PolicyEvaluationStage.EVENT_INGEST && context.getCurrentEvent() != null) {
            TraceEvent event = context.getCurrentEvent();
            if ("TOOL_CALL".equals(event.getEventType()) && blockedTools.contains(event.getEventName())) {
                return PolicyResult.violated(Map.of(
                        "blocked_tool", event.getEventName(),
                        "blocked_tools", blockedTools
                ));
            }
            return PolicyResult.passed();
        }

        for (TraceEvent event : context.getEvents()) {
            if ("TOOL_CALL".equals(event.getEventType())
                    && blockedTools.contains(event.getEventName())) {
                return PolicyResult.violated(Map.of(
                        "blocked_tool", event.getEventName(),
                        "blocked_tools", blockedTools
                ));
            }
        }
        return PolicyResult.passed();
    }
}
