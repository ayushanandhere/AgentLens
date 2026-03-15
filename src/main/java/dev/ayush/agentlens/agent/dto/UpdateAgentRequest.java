package dev.ayush.agentlens.agent.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAgentRequest {

    private String name;

    private String description;

    @Pattern(regexp = "ACTIVE|PAUSED|KILLED", message = "Status must be ACTIVE, PAUSED, or KILLED")
    private String status;
}
