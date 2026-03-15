package dev.ayush.agentlens.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResponse {

    private Long totalTraces;
    private Long completedTraces;
    private Long failedTraces;
    private Long blockedTraces;
    private Double avgLatencyMs;
    private BigDecimal avgCostUsd;
    private BigDecimal totalCostUsd;
    private Double avgGroundingScore;
    private Double violationRatePercent;
}
