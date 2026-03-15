package dev.ayush.agentlens.trace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceTimelineResponse {

    private UUID traceId;
    private UUID agentId;
    private String agentName;
    private String status;
    private String policyResult;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long totalDurationMs;
    private List<TraceTimelineItemResponse> items;
}
