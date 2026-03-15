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
public class CostBreakdownEntry {

    private String key;
    private String label;
    private BigDecimal totalCost;
    private Long totalTokens;
    private Long traceCount;
}
