package dev.ayush.agentlens.kafka;

import dev.ayush.agentlens.agent.Agent;
import dev.ayush.agentlens.config.KafkaConfig;
import dev.ayush.agentlens.kafka.event.TraceCompletedEvent;
import dev.ayush.agentlens.trace.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class TraceEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTraceCompleted(Trace trace, Agent agent, int toolCallCount) {
        TraceCompletedEvent event = TraceCompletedEvent.builder()
                .traceId(trace.getId().toString())
                .agentId(agent.getId().toString())
                .agentName(agent.getName())
                .tenantId(trace.getTenantId())
                .model(trace.getModel())
                .totalTokens(trace.getTotalTokens())
                .estimatedCost(trace.getEstimatedCost())
                .latencyMs(trace.getLatencyMs())
                .policyResult(trace.getPolicyResult())
                .groundingScore(trace.getGroundingScore())
                .toolCallCount(toolCallCount)
                .status(trace.getStatus())
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(KafkaConfig.TOPIC_AGENT_TRACES, trace.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish trace completed event for trace {}", trace.getId(), ex);
                    } else {
                        log.info("Published trace completed event for trace {} to {}", trace.getId(), KafkaConfig.TOPIC_AGENT_TRACES);
                    }
                });
    }
}
