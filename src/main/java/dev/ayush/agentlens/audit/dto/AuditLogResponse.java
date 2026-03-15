package dev.ayush.agentlens.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private LocalDateTime timestamp;
    private String actor;
    private String action;
    private String resourceType;
    private String resourceId;
    private Map<String, Object> details;
    private String ipAddress;
}
