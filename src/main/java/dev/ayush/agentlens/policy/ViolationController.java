package dev.ayush.agentlens.policy;

import dev.ayush.agentlens.policy.dto.ApproveViolationRequest;
import dev.ayush.agentlens.policy.dto.ViolationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/violations")
@RequiredArgsConstructor
public class ViolationController {

    private final PolicyService policyService;

    @GetMapping
    public Page<ViolationResponse> listViolations(
            @RequestParam(required = false) UUID traceId,
            @RequestParam(required = false) UUID policyId,
            @RequestParam(required = false) String severity,
            Pageable pageable) {
        return policyService.listViolations(traceId, policyId, severity, pageable);
    }

    @GetMapping("/{id}")
    public ViolationResponse getViolation(@PathVariable UUID id) {
        return policyService.getViolation(id);
    }

    @PostMapping("/{id}/approve")
    public ViolationResponse approveViolation(@PathVariable UUID id,
                                              @Valid @RequestBody ApproveViolationRequest request) {
        return policyService.approveViolation(id, request.getResolvedBy());
    }
}
