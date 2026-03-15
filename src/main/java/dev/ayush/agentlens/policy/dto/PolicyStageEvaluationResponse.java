package dev.ayush.agentlens.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyStageEvaluationResponse {

    private String stage;
    private String verdict;
    private List<PolicyMatchResponse> violations;
}
