package dev.ayush.agentlens.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {

    private UUID id;
    private String name;
    private String description;
    private String owner;
    private String status;
    private Long traceCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
