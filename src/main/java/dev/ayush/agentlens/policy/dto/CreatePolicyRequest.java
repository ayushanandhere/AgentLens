package dev.ayush.agentlens.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "TOKEN_BUDGET|COST_BUDGET|TOOL_BLOCK|RATE_LIMIT|PII_CHECK|REQUIRE_APPROVAL",
            message = "Unsupported policyType")
    private String policyType;

    @NotNull(message = "config is required")
    private Map<String, Object> config;

    @Pattern(regexp = "GLOBAL|AGENT|TENANT", message = "scope must be GLOBAL, AGENT, or TENANT")
    private String scope = "GLOBAL";
    private String scopeId;
    @Pattern(regexp = "BLOCK|WARN|LOG", message = "severity must be BLOCK, WARN, or LOG")
    private String severity = "BLOCK";
}
