package dev.ayush.agentlens.policy;

import dev.ayush.agentlens.policy.dto.CreatePolicyRequest;
import dev.ayush.agentlens.policy.dto.PolicyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyResponse createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        return policyService.createPolicy(request);
    }

    @GetMapping
    public Page<PolicyResponse> listPolicies(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) Boolean enabled,
            Pageable pageable) {
        return policyService.listPolicies(type, scope, enabled, pageable);
    }

    @GetMapping("/{id}")
    public PolicyResponse getPolicy(@PathVariable UUID id) {
        return policyService.getPolicy(id);
    }

    @PutMapping("/{id}")
    public PolicyResponse updatePolicy(@PathVariable UUID id,
                                       @Valid @RequestBody CreatePolicyRequest request) {
        return policyService.updatePolicy(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePolicy(@PathVariable UUID id) {
        policyService.deletePolicy(id);
    }
}
