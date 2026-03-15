package dev.ayush.agentlens.trace.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartTraceRequest {

    @NotNull(message = "agentId is required")
    private UUID agentId;

    private String model;
    private String promptText;
    private BigDecimal temperature;
    private String tenantId;
    private String sessionId;
    private Map<String, Object> metadata;
}
