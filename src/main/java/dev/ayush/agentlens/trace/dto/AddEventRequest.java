package dev.ayush.agentlens.trace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddEventRequest {

    @NotBlank(message = "eventType is required")
    @Pattern(regexp = "TOOL_CALL|RETRIEVAL|LLM_CALL|GUARDRAIL_CHECK|HUMAN_APPROVAL",
            message = "eventType must be one of: TOOL_CALL, RETRIEVAL, LLM_CALL, GUARDRAIL_CHECK, HUMAN_APPROVAL")
    private String eventType;

    @NotBlank(message = "eventName is required")
    private String eventName;

    private Map<String, Object> inputData;
    private Map<String, Object> outputData;
    private String status;
    private Long durationMs;
    private String errorMessage;
}
