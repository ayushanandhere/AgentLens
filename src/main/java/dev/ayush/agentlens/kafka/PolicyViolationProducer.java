package dev.ayush.agentlens.kafka;

import dev.ayush.agentlens.config.KafkaConfig;
import dev.ayush.agentlens.kafka.event.PolicyViolationEvent;
import dev.ayush.agentlens.policy.Policy;
import dev.ayush.agentlens.policy.PolicyViolation;
import dev.ayush.agentlens.trace.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyViolationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishViolation(PolicyViolation violation, Policy policy, Trace trace) {
        PolicyViolationEvent event = PolicyViolationEvent.builder()
                .traceId(trace.getId().toString())
                .agentId(trace.getAgent().getId().toString())
                .policyId(policy.getId().toString())
                .policyName(policy.getName())
                .violationType(violation.getViolationType())
                .severity(violation.getSeverity())
                .details(violation.getDetails())
                .actionTaken(violation.getActionTaken())
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(KafkaConfig.TOPIC_POLICY_VIOLATIONS, trace.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish violation event for trace {}", trace.getId(), ex);
                    } else {
                        log.info("Published policy violation event [{}] for trace {}", violation.getViolationType(), trace.getId());
                    }
                });
    }
}
