package dev.ayush.agentlens.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatencyResponse {

    private Double p50;
    private Double p95;
    private Double p99;
    private Double avg;
    private Double min;
    private Double max;
}
