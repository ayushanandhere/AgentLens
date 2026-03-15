package dev.ayush.agentlens.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationResponse {

    private UUID id;
    private UUID traceId;
    private UUID policyId;
    private String policyName;
    private String violationType;
    private String severity;
    private Map<String, Object> details;
    private String actionTaken;
    private String resolvedBy;
    private LocalDateTime createdAt;
}
