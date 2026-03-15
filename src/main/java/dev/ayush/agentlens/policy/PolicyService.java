package dev.ayush.agentlens.policy;

import dev.ayush.agentlens.agent.Agent;
import dev.ayush.agentlens.agent.AgentRepository;
import dev.ayush.agentlens.common.exception.ResourceNotFoundException;
import dev.ayush.agentlens.common.model.PolicyVerdict;
import dev.ayush.agentlens.common.model.TraceStatus;
import dev.ayush.agentlens.common.model.ViolationAction;
import dev.ayush.agentlens.common.util.CostCalculator;
import dev.ayush.agentlens.kafka.AuditEventProducer;
import dev.ayush.agentlens.policy.dto.*;
import dev.ayush.agentlens.policy.engine.PolicyEngine;
import dev.ayush.agentlens.policy.engine.PolicyEngineResult;
import dev.ayush.agentlens.policy.engine.PolicyEvaluationStage;
import dev.ayush.agentlens.policy.engine.PolicyType;
import dev.ayush.agentlens.trace.Trace;
import dev.ayush.agentlens.trace.TraceEvent;
import dev.ayush.agentlens.trace.TraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyViolationRepository violationRepository;
    private final AuditEventProducer auditEventProducer;
    private final AgentRepository agentRepository;
    private final TraceRepository traceRepository;
    private final PolicyEngine policyEngine;
    private final CostCalculator costCalculator;

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
        Specification<Policy> spec = PolicySpecification.withFilters(type, scope, enabled);
        return policyRepository.findAll(spec, pageable).map(this::toPolicyResponse);
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
    public Page<ViolationResponse> listViolations(UUID traceId, UUID policyId, String severity,
                                                  String actionTaken, Pageable pageable) {
        Specification<PolicyViolation> spec = PolicyViolationSpecification.withFilters(traceId, policyId, severity, actionTaken);
        return violationRepository.findAll(spec, pageable).map(this::toViolationResponse);
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
        ensureApproverAllowed(v, resolvedBy);
        v.setResolvedBy(resolvedBy);
        v.setActionTaken(ViolationAction.APPROVED_OVERRIDE);
        v = violationRepository.save(v);

        Trace trace = v.getTrace();
        if (TraceStatus.PENDING_APPROVAL.equals(trace.getStatus()) && !hasPendingApprovals(trace.getId())) {
            trace.setStatus(TraceStatus.RUNNING);
            trace.setPolicyResult(PolicyVerdict.WARN);
            trace.setBlockedReason(null);
            traceRepository.save(trace);
            auditEventProducer.publishTraceApproved(trace, resolvedBy);
        }

        auditEventProducer.publishViolationApproved(v, resolvedBy);
        return toViolationResponse(v);
    }

    @Transactional
    public ViolationResponse rejectViolation(UUID id, String resolvedBy, String reason) {
        PolicyViolation violation = violationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PolicyViolation", id));
        ensureApproverAllowed(violation, resolvedBy);
        violation.setResolvedBy(resolvedBy);
        violation.setActionTaken(ViolationAction.REJECTED);
        if (reason != null && !reason.isBlank()) {
            Map<String, Object> details = new LinkedHashMap<>();
            if (violation.getDetails() != null) {
                details.putAll(violation.getDetails());
            }
            details.put("rejection_reason", reason);
            violation.setDetails(details);
        }
        violation = violationRepository.save(violation);

        Trace trace = violation.getTrace();
        trace.setStatus(TraceStatus.BLOCKED);
        trace.setPolicyResult(PolicyVerdict.FAIL);
        trace.setBlockedReason(reason != null && !reason.isBlank() ? reason :
                "Approval rejected for policy " + violation.getPolicy().getName());
        traceRepository.save(trace);
        auditEventProducer.publishTraceRejected(trace, resolvedBy, trace.getBlockedReason());

        return toViolationResponse(violation);
    }

    @Transactional(readOnly = true)
    public EvaluatePoliciesResponse evaluatePolicies(EvaluatePoliciesRequest request) {
        Agent agent = agentRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent", request.getAgentId()));

        Trace trace = Trace.builder()
                .id(UUID.randomUUID())
                .agent(agent)
                .traceId(UUID.randomUUID().toString().replace("-", ""))
                .status(TraceStatus.RUNNING)
                .model(request.getModel())
                .promptText(request.getPromptText())
                .responseText(request.getResponseText())
                .temperature(request.getTemperature())
                .tenantId(request.getTenantId())
                .sessionId(request.getSessionId())
                .metadata(request.getMetadata())
                .startedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        if (request.getInputTokens() != null && request.getOutputTokens() != null) {
            trace.setInputTokens(request.getInputTokens());
            trace.setOutputTokens(request.getOutputTokens());
            trace.setTotalTokens(request.getInputTokens() + request.getOutputTokens());
            trace.setEstimatedCost(costCalculator.calculateCost(
                    trace.getModel(), request.getInputTokens(), request.getOutputTokens()));
            trace.setGroundingScore(request.getGroundingScore());
            trace.setTtftMs(request.getTtftMs());
        }

        List<TraceEvent> allEvents = new ArrayList<>();
        List<PolicyMatchResponse> eventViolations = new ArrayList<>();
        String eventVerdict = PolicyVerdict.PASS;
        if (request.getEvents() != null) {
            int sequence = 1;
            for (EvaluatePolicyEventRequest eventRequest : request.getEvents()) {
                TraceEvent event = TraceEvent.builder()
                        .id(UUID.randomUUID())
                        .trace(trace)
                        .eventType(eventRequest.getEventType())
                        .eventName(eventRequest.getEventName())
                        .inputData(eventRequest.getInputData())
                        .outputData(eventRequest.getOutputData())
                        .status(eventRequest.getStatus())
                        .durationMs(eventRequest.getDurationMs())
                        .errorMessage(eventRequest.getErrorMessage())
                        .sequenceNum(sequence++)
                        .timestamp(LocalDateTime.now())
                        .build();
                allEvents.add(event);
                PolicyEngineResult eventResult = policyEngine.dryRun(
                        PolicyEvaluationStage.EVENT_INGEST, trace, List.copyOf(allEvents), event, agent);
                eventVerdict = mergePolicyVerdict(eventVerdict, eventResult.getVerdict());
                eventViolations.addAll(eventResult.getViolations().stream().map(this::toPolicyMatchResponse).toList());
            }
        }

        PolicyEngineResult pre = policyEngine.dryRun(
                PolicyEvaluationStage.PRE_EXECUTION, trace, List.of(), null, agent);
        PolicyEngineResult completion = policyEngine.dryRun(
                PolicyEvaluationStage.COMPLETION, trace, allEvents, null, agent);

        List<PolicyStageEvaluationResponse> stages = List.of(
                PolicyStageEvaluationResponse.builder()
                        .stage(PolicyEvaluationStage.PRE_EXECUTION.name())
                        .verdict(pre.getVerdict())
                        .violations(pre.getViolations().stream().map(this::toPolicyMatchResponse).toList())
                        .build(),
                PolicyStageEvaluationResponse.builder()
                        .stage(PolicyEvaluationStage.EVENT_INGEST.name())
                        .verdict(eventVerdict)
                        .violations(eventViolations)
                        .build(),
                PolicyStageEvaluationResponse.builder()
                        .stage(PolicyEvaluationStage.COMPLETION.name())
                        .verdict(completion.getVerdict())
                        .violations(completion.getViolations().stream().map(this::toPolicyMatchResponse).toList())
                        .build()
        );

        String overallVerdict = PolicyVerdict.PASS;
        for (PolicyStageEvaluationResponse stage : stages) {
            overallVerdict = mergePolicyVerdict(overallVerdict, stage.getVerdict());
        }

        return EvaluatePoliciesResponse.builder()
                .overallVerdict(overallVerdict)
                .stages(stages)
                .build();
    }

    private boolean hasPendingApprovals(UUID traceId) {
        return violationRepository.findByTraceId(traceId).stream()
                .anyMatch(v -> ViolationAction.PENDING_APPROVAL.equals(v.getActionTaken()));
    }

    private void ensureApproverAllowed(PolicyViolation violation, String resolvedBy) {
        Object approversConfig = violation.getPolicy().getConfig().get("approvers");
        if (!(approversConfig instanceof List<?> approvers) || approvers.isEmpty()) {
            return;
        }
        boolean allowed = approvers.stream()
                .map(String::valueOf)
                .anyMatch(resolvedBy::equalsIgnoreCase);
        if (!allowed) {
            throw new IllegalArgumentException("Approver '" + resolvedBy + "' is not allowed for this policy");
        }
    }

    private String mergePolicyVerdict(String current, String candidate) {
        return rank(candidate) > rank(current) ? candidate : current;
    }

    private int rank(String verdict) {
        if (PolicyVerdict.FAIL.equals(verdict)) {
            return 4;
        }
        if (PolicyVerdict.PENDING_APPROVAL.equals(verdict)) {
            return 3;
        }
        if (PolicyVerdict.WARN.equals(verdict)) {
            return 2;
        }
        if (PolicyVerdict.PASS.equals(verdict)) {
            return 1;
        }
        return 0;
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

    private PolicyMatchResponse toPolicyMatchResponse(PolicyViolation violation) {
        return PolicyMatchResponse.builder()
                .policyName(violation.getPolicy().getName())
                .policyType(violation.getViolationType())
                .severity(violation.getSeverity())
                .actionTaken(violation.getActionTaken())
                .details(violation.getDetails())
                .build();
    }
}
