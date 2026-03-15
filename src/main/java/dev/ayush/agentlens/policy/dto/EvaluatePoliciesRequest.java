package dev.ayush.agentlens.policy.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluatePoliciesRequest {

    @NotNull(message = "agentId is required")
    private UUID agentId;

    private String model;
    private String promptText;
    private String responseText;
    private BigDecimal temperature;
    private Integer inputTokens;
    private Integer outputTokens;
    private Long ttftMs;
    private BigDecimal groundingScore;
    private String tenantId;
    private String sessionId;
    private Map<String, Object> metadata;
    private List<EvaluatePolicyEventRequest> events;
}
