package dev.ayush.agentlens.trace.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteTraceRequest {

    private String responseText;

    @NotNull(message = "inputTokens is required")
    private Integer inputTokens;

    @NotNull(message = "outputTokens is required")
    private Integer outputTokens;

    private BigDecimal groundingScore;
    private Long ttftMs;
}
