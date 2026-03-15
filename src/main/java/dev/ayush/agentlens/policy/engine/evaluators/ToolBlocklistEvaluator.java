package dev.ayush.agentlens.policy.engine.evaluators;

import dev.ayush.agentlens.policy.engine.*;
import dev.ayush.agentlens.trace.TraceEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ToolBlocklistEvaluator implements PolicyEvaluator {

    @Override
    public PolicyType getType() {
        return PolicyType.TOOL_BLOCK;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PolicyResult evaluate(Map<String, Object> config, TraceContext context) {
        List<String> blockedTools = (List<String>) config.get("blocked_tools");
        if (blockedTools == null || blockedTools.isEmpty()) {
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
