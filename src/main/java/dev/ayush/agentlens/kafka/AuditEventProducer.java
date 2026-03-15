package dev.ayush.agentlens.kafka;

import dev.ayush.agentlens.agent.Agent;
import dev.ayush.agentlens.config.KafkaConfig;
import dev.ayush.agentlens.kafka.event.AuditEvent;
import dev.ayush.agentlens.policy.PolicyViolation;
import dev.ayush.agentlens.trace.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String actor, String action, String resourceType, String resourceId, Map<String, Object> details) {
        AuditEvent event = AuditEvent.builder()
                .actor(actor)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .details(details)
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(KafkaConfig.TOPIC_AUDIT_EVENTS, resourceId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish audit event [{}/{}]", action, resourceId, ex);
                    } else {
                        log.debug("Published audit event [{}/{}] to {}", action, resourceId, KafkaConfig.TOPIC_AUDIT_EVENTS);
                    }
                });
    }

    public void publishTraceStarted(Trace trace) {
        publish("system:trace-service", "TRACE_STARTED", "TRACE", trace.getId().toString(),
                Map.of("agent_id", trace.getAgent().getId().toString(), "model", nullSafe(trace.getModel())));
    }

    public void publishTraceCompleted(Trace trace) {
        publish("system:trace-service", "TRACE_COMPLETED", "TRACE", trace.getId().toString(),
                Map.of("agent_id", trace.getAgent().getId().toString(),
                        "total_tokens", nullSafe(trace.getTotalTokens()),
                        "estimated_cost", nullSafe(trace.getEstimatedCost()),
                        "policy_result", nullSafe(trace.getPolicyResult())));
    }

    public void publishTraceBlocked(Trace trace, String reason) {
        publish("system:policy-engine", "TRACE_BLOCKED", "TRACE", trace.getId().toString(),
                Map.of("agent_id", trace.getAgent().getId().toString(), "reason", reason));
    }

    public void publishPolicyViolated(PolicyViolation violation, Trace trace) {
        publish("system:policy-engine", "POLICY_VIOLATED", "TRACE", trace.getId().toString(),
                Map.of("violation_id", violation.getId().toString(),
                        "violation_type", violation.getViolationType(),
                        "severity", violation.getSeverity()));
    }

    public void publishAgentCreated(Agent agent) {
        publish("user:api", "AGENT_CREATED", "AGENT", agent.getId().toString(),
                Map.of("name", agent.getName()));
    }

    public void publishAgentUpdated(Agent agent) {
        publish("user:api", "AGENT_UPDATED", "AGENT", agent.getId().toString(),
                Map.of("name", agent.getName(), "status", agent.getStatus()));
    }

    public void publishAgentKilled(Agent agent) {
        publish("user:api", "AGENT_KILLED", "AGENT", agent.getId().toString(),
                Map.of("name", agent.getName()));
    }

    public void publishViolationApproved(PolicyViolation violation, String approvedBy) {
        publish("user:" + approvedBy, "VIOLATION_APPROVED", "VIOLATION", violation.getId().toString(),
                Map.of("trace_id", violation.getTrace().getId().toString(),
                        "violation_type", violation.getViolationType()));
    }

    private Object nullSafe(Object value) {
        return value != null ? value : "N/A";
    }
}
