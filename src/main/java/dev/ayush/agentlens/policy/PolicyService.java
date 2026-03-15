package dev.ayush.agentlens.policy;

import dev.ayush.agentlens.common.exception.ResourceNotFoundException;
import dev.ayush.agentlens.kafka.AuditEventProducer;
import dev.ayush.agentlens.policy.dto.CreatePolicyRequest;
import dev.ayush.agentlens.policy.dto.PolicyResponse;
import dev.ayush.agentlens.policy.dto.ViolationResponse;
import dev.ayush.agentlens.policy.engine.PolicyType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyViolationRepository violationRepository;
    private final AuditEventProducer auditEventProducer;

    @Transactional
    public PolicyResponse createPolicy(CreatePolicyRequest request) {
        // Validate policyType is a valid enum value
        PolicyType.valueOf(request.getPolicyType());

        Policy policy = Policy.builder()
                .name(request.getName())
                .description(request.getDescription())
                .policyType(request.getPolicyType())
                .config(request.getConfig())
                .scope(request.getScope() != null ? request.getScope() : "GLOBAL")
                .scopeId(request.getScopeId())
                .severity(request.getSeverity() != null ? request.getSeverity() : "BLOCK")
                .build();

        policy = policyRepository.save(policy);
        return toPolicyResponse(policy);
    }

    @Transactional(readOnly = true)
    public Page<PolicyResponse> listPolicies(String type, String scope, Boolean enabled, Pageable pageable) {
        return policyRepository.findAll(pageable).map(this::toPolicyResponse);
    }

    @Transactional(readOnly = true)
    public PolicyResponse getPolicy(UUID id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", id));
        return toPolicyResponse(policy);
    }

    @Transactional
    public PolicyResponse updatePolicy(UUID id, CreatePolicyRequest request) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", id));

        PolicyType.valueOf(request.getPolicyType());

        policy.setName(request.getName());
        policy.setDescription(request.getDescription());
        policy.setPolicyType(request.getPolicyType());
        policy.setConfig(request.getConfig());
        policy.setScope(request.getScope() != null ? request.getScope() : policy.getScope());
        policy.setScopeId(request.getScopeId());
        policy.setSeverity(request.getSeverity() != null ? request.getSeverity() : policy.getSeverity());
        policy.setUpdatedAt(LocalDateTime.now());

        policy = policyRepository.save(policy);
        return toPolicyResponse(policy);
    }

    @Transactional
    public void deletePolicy(UUID id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", id));
        policy.setEnabled(false);
        policy.setUpdatedAt(LocalDateTime.now());
        policyRepository.save(policy);
    }

    // --- Violations ---

    @Transactional(readOnly = true)
    public Page<ViolationResponse> listViolations(UUID traceId, UUID policyId, String severity, Pageable pageable) {
        return violationRepository.findAll(pageable).map(this::toViolationResponse);
    }

    @Transactional(readOnly = true)
    public ViolationResponse getViolation(UUID id) {
        PolicyViolation v = violationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PolicyViolation", id));
        return toViolationResponse(v);
    }

    @Transactional
    public ViolationResponse approveViolation(UUID id, String resolvedBy) {
        PolicyViolation v = violationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PolicyViolation", id));
        v.setResolvedBy(resolvedBy);
        v.setActionTaken("APPROVED_OVERRIDE");
        v = violationRepository.save(v);
        auditEventProducer.publishViolationApproved(v, resolvedBy);
        return toViolationResponse(v);
    }

    private PolicyResponse toPolicyResponse(Policy p) {
        return PolicyResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .policyType(p.getPolicyType())
                .config(p.getConfig())
                .scope(p.getScope())
                .scopeId(p.getScopeId())
                .enabled(p.getEnabled())
                .severity(p.getSeverity())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private ViolationResponse toViolationResponse(PolicyViolation v) {
        return ViolationResponse.builder()
                .id(v.getId())
                .traceId(v.getTrace().getId())
                .policyId(v.getPolicy().getId())
                .policyName(v.getPolicy().getName())
                .violationType(v.getViolationType())
                .severity(v.getSeverity())
                .details(v.getDetails())
                .actionTaken(v.getActionTaken())
                .resolvedBy(v.getResolvedBy())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
