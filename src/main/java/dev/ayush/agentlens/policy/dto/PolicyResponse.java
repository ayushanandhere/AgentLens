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
public class PolicyResponse {

    private UUID id;
    private String name;
    private String description;
    private String policyType;
    private Map<String, Object> config;
    private String scope;
    private String scopeId;
    private Boolean enabled;
    private String severity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
