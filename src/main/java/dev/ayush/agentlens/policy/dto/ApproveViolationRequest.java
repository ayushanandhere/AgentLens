package dev.ayush.agentlens.policy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveViolationRequest {

    @NotBlank(message = "resolvedBy is required")
    private String resolvedBy;
}
