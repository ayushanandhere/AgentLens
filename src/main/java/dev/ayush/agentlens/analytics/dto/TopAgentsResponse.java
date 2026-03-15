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
public class TopAgentsResponse {

    private List<TopAgentEntry> mostActive;
    private List<TopAgentEntry> mostExpensive;
    private List<TopAgentEntry> mostViolations;
}
