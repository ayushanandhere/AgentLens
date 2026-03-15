package dev.ayush.agentlens.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationTrendsResponse {

    private List<ViolationTypeCount> byType;
    private List<ViolationSeverityCount> bySeverity;
    private List<ViolationDayCount> byDay;
}
