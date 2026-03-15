package dev.ayush.agentlens.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyViolationEvent {

    private String traceId;
    private String agentId;
    private String policyId;
    private String policyName;
    private String violationType;
    private String severity;
    private Map<String, Object> details;
    private String actionTaken;
    private Instant timestamp;
}
