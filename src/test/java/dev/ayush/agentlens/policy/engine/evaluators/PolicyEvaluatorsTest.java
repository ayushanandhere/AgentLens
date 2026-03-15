package dev.ayush.agentlens.policy.engine.evaluators;

import dev.ayush.agentlens.agent.Agent;
import dev.ayush.agentlens.policy.engine.PolicyEvaluationStage;
import dev.ayush.agentlens.policy.engine.TraceContext;
import dev.ayush.agentlens.trace.Trace;
import dev.ayush.agentlens.trace.TraceEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyEvaluatorsTest {

    @Test
    void requireApprovalFlagsConfiguredToolDuringEventIngest() {
        RequireApprovalEvaluator evaluator = new RequireApprovalEvaluator();
        TraceEvent event = TraceEvent.builder()
                .eventType("TOOL_CALL")
                .eventName("execShell")
                .sequenceNum(1)
                .timestamp(LocalDateTime.now())
                .build();

        TraceContext context = TraceContext.builder()
                .trace(baseTrace())
                .agent(baseAgent())
                .events(List.of(event))
                .currentEvent(event)
                .stage(PolicyEvaluationStage.EVENT_INGEST)
                .dryRun(true)
                .build();

        assertThat(evaluator.evaluate(Map.of("for_tools", List.of("execShell")), context).violated()).isTrue();
    }

    @Test
    void toolBlocklistOnlyChecksCurrentEventDuringEventIngest() {
        ToolBlocklistEvaluator evaluator = new ToolBlocklistEvaluator();
        TraceEvent event = TraceEvent.builder()
                .eventType("TOOL_CALL")
                .eventName("queryDatabase")
                .sequenceNum(2)
                .timestamp(LocalDateTime.now())
                .build();

        TraceContext context = TraceContext.builder()
                .trace(baseTrace())
                .agent(baseAgent())
                .events(List.of(event))
                .currentEvent(event)
                .stage(PolicyEvaluationStage.EVENT_INGEST)
                .dryRun(true)
                .build();

        assertThat(evaluator.evaluate(Map.of("blocked_tools", List.of("execShell")), context).violated()).isFalse();
    }

    @Test
    void piiEvaluatorFindsSensitiveDataInEventPayloads() {
        PiiDetectionEvaluator evaluator = new PiiDetectionEvaluator();
        TraceEvent event = TraceEvent.builder()
                .eventType("TOOL_CALL")
                .eventName("sendNotification")
                .inputData(Map.of("email", "user@example.com"))
                .sequenceNum(1)
                .timestamp(LocalDateTime.now())
                .build();

        TraceContext context = TraceContext.builder()
                .trace(baseTrace())
                .agent(baseAgent())
                .events(List.of(event))
                .currentEvent(event)
                .stage(PolicyEvaluationStage.EVENT_INGEST)
                .dryRun(true)
                .build();

        assertThat(evaluator.evaluate(Map.of(), context).violated()).isTrue();
    }

    private Trace baseTrace() {
        return Trace.builder()
                .id(UUID.randomUUID())
                .traceId("trace-id")
                .status("RUNNING")
                .startedAt(LocalDateTime.now())
                .build();
    }

    private Agent baseAgent() {
        return Agent.builder()
                .id(UUID.randomUUID())
                .name("demo-agent")
                .status("ACTIVE")
                .build();
    }
}
