package dev.ayush.agentlens.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    private String actor;
    private String action;
    private String resourceType;
    private String resourceId;
    private Map<String, Object> details;
    private Instant timestamp;
}
