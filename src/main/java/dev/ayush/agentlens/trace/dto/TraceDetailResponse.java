package dev.ayush.agentlens.trace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceDetailResponse {

    private UUID id;
    private UUID agentId;
    private String agentName;
    private String traceId;
    private String status;
    private String model;
    private String promptText;
    private String responseText;
    private BigDecimal temperature;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private BigDecimal estimatedCost;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long latencyMs;
    private Long ttftMs;
    private BigDecimal groundingScore;
    private String policyResult;
    private String blockedReason;
    private String tenantId;
    private String sessionId;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private List<TraceEventResponse> events;
}
