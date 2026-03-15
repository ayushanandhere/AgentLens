package dev.ayush.agentlens.policy.engine;

import dev.ayush.agentlens.policy.PolicyViolation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PolicyEngineResult {
    private final String verdict; // "PASS", "WARN", or "FAIL"
    private final List<PolicyViolation> violations;
}
