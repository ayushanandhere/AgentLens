package dev.ayush.agentlens.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceCompletedEvent {

    private String traceId;
    private String agentId;
    private String agentName;
    private String tenantId;
    private String model;
    private Integer totalTokens;
    private BigDecimal estimatedCost;
    private Long latencyMs;
    private String policyResult;
    private BigDecimal groundingScore;
    private Integer toolCallCount;
    private String status;
    private Instant timestamp;
}
