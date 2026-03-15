package dev.ayush.agentlens.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyMatchResponse {

    private String policyName;
    private String policyType;
    private String severity;
    private String actionTaken;
    private Map<String, Object> details;
}
