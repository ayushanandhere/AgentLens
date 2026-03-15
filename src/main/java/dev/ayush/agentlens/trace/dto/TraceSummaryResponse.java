package dev.ayush.agentlens.trace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceSummaryResponse {

    private UUID id;
    private UUID agentId;
    private String agentName;
    private String model;
    private String status;
    private String policyResult;
    private Integer totalTokens;
    private BigDecimal estimatedCost;
    private Long latencyMs;
    private BigDecimal groundingScore;
    private LocalDateTime startedAt;
}
