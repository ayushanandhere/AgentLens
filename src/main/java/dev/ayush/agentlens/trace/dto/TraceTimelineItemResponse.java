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
public class TraceTimelineItemResponse {

    private UUID eventId;
    private String eventType;
    private String eventName;
    private String status;
    private LocalDateTime timestamp;
    private Long relativeStartMs;
    private Long durationMs;
    private String errorMessage;
    private Map<String, Object> inputData;
    private Map<String, Object> outputData;
}
