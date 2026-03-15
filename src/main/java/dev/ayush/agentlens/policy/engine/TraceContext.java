package dev.ayush.agentlens.policy.engine;

import dev.ayush.agentlens.agent.Agent;
import dev.ayush.agentlens.trace.Trace;
import dev.ayush.agentlens.trace.TraceEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class TraceContext {
    private final Trace trace;
    private final List<TraceEvent> events;
    private final Agent agent;
}
