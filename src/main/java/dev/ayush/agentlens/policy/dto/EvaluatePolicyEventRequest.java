package dev.ayush.agentlens.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluatePolicyEventRequest {

    private String eventType;
    private String eventName;
    private Map<String, Object> inputData;
    private Map<String, Object> outputData;
    private String status;
    private Long durationMs;
    private String errorMessage;
}
