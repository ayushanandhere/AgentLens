package dev.ayush.agentlens.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePolicyRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    @NotBlank(message = "policyType is required")
    private String policyType;

    @NotNull(message = "config is required")
    private Map<String, Object> config;

    private String scope = "GLOBAL";
    private String scopeId;
    private String severity = "BLOCK";
}
