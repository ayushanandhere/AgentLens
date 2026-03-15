package dev.ayush.agentlens.trace.dto;

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
public class TraceEventResponse {

    private UUID id;
    private UUID traceId;
    private String eventType;
    private String eventName;
    private Map<String, Object> inputData;
    private Map<String, Object> outputData;
    private String status;
    private Long durationMs;
    private String errorMessage;
    private Integer sequenceNum;
    private LocalDateTime timestamp;
}
