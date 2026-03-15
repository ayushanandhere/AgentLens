package dev.ayush.agentlens.policy.engine;

import dev.ayush.agentlens.agent.Agent;
import dev.ayush.agentlens.common.model.PolicyVerdict;
import dev.ayush.agentlens.common.model.ViolationAction;
import dev.ayush.agentlens.policy.Policy;
import dev.ayush.agentlens.policy.PolicyRepository;
import dev.ayush.agentlens.kafka.AuditEventProducer;
import dev.ayush.agentlens.kafka.PolicyViolationProducer;
import dev.ayush.agentlens.policy.PolicyViolation;
import dev.ayush.agentlens.policy.PolicyViolationRepository;
import dev.ayush.agentlens.trace.Trace;
import dev.ayush.agentlens.trace.TraceEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyEngine {

    private final PolicyRepository policyRepository;
    private final PolicyViolationRepository violationRepository;
    private final PolicyEvaluatorRegistry evaluatorRegistry;
    private final PolicyViolationProducer policyViolationProducer;
    private final AuditEventProducer auditEventProducer;
    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    public PolicyEngineResult evaluatePreExecution(Trace trace, Agent agent) {
        TraceContext context = TraceContext.builder()
                .trace(trace)
                .events(Collections.emptyList())
                .agent(agent)
                .stage(PolicyEvaluationStage.PRE_EXECUTION)
                .dryRun(false)
                .build();
        return evaluateStage(context, true);
    }

    public PolicyEngineResult evaluateEventIngest(Trace trace, List<TraceEvent> events, TraceEvent currentEvent, Agent agent) {
        TraceContext context = TraceContext.builder()
                .trace(trace)
                .events(events)
                .currentEvent(currentEvent)
                .agent(agent)
                .stage(PolicyEvaluationStage.EVENT_INGEST)
                .dryRun(false)
                .build();
        return evaluateStage(context, true);
    }

    public PolicyEngineResult evaluatePostCompletion(Trace trace, List<TraceEvent> events, Agent agent) {
        TraceContext context = TraceContext.builder()
                .trace(trace)
                .events(events)
                .agent(agent)
                .stage(PolicyEvaluationStage.COMPLETION)
                .dryRun(false)
                .build();
        return evaluateStage(context, true);
    }

    public PolicyEngineResult dryRun(PolicyEvaluationStage stage, Trace trace, List<TraceEvent> events,
                                     TraceEvent currentEvent, Agent agent) {
        TraceContext context = TraceContext.builder()
                .trace(trace)
                .events(events != null ? events : Collections.emptyList())
                .currentEvent(currentEvent)
                .agent(agent)
                .stage(stage)
                .dryRun(true)
                .build();
        return evaluateStage(context, false);
    }

    private PolicyEngineResult evaluateStage(TraceContext context, boolean persistViolations) {
        Span span = tracer.spanBuilder("policy.evaluate")
                .setAttribute("policy.stage", context.getStage().name())
                .setAttribute("policy.dry_run", context.isDryRun())
                .setAttribute("policy.trace_id", context.getTrace().getId().toString())
                .startSpan();
        try (var ignored = span.makeCurrent()) {
        List<Policy> policies = loadApplicablePolicies(context.getAgent(), context.getTrace()).stream()
                .filter(policy -> evaluatorRegistry.getEvaluator(PolicyType.valueOf(policy.getPolicyType()))
                        .stages()
                        .contains(context.getStage()))
                .toList();

        List<PolicyViolation> violations = new ArrayList<>();
        boolean hasBlock = false;
        boolean hasPendingApproval = false;
        boolean hasWarn = false;

        for (Policy policy : policies) {
            PolicyType type = PolicyType.valueOf(policy.getPolicyType());
            PolicyEvaluator evaluator = evaluatorRegistry.getEvaluator(type);
            PolicyResult result = evaluator.evaluate(policy.getConfig(), context);

            if (result.violated()) {
                PolicySeverity severity = PolicySeverity.valueOf(policy.getSeverity());
                String actionTaken = actionFor(type, severity);
                log.warn("Policy violation: {} [{}] on trace {}", policy.getName(), severity, context.getTrace().getId());

                PolicyViolation violation = PolicyViolation.builder()
                        .trace(context.getTrace())
                        .policy(policy)
                        .violationType(policy.getPolicyType())
                        .severity(policy.getSeverity())
                        .details(result.details())
                        .actionTaken(actionTaken)
                        .build();

                if (persistViolations) {
                    violation = violationRepository.save(violation);
                    policyViolationProducer.publishViolation(violation, policy, context.getTrace());
                    auditEventProducer.publishPolicyViolated(violation, context.getTrace());
                }
                violations.add(violation);
                meterRegistry.counter("agentlens.policy.violation",
                        "type", policy.getPolicyType(),
                        "stage", context.getStage().name(),
                        "action", actionTaken).increment();

                if (ViolationAction.PENDING_APPROVAL.equals(actionTaken)) {
                    hasPendingApproval = true;
                } else if (severity == PolicySeverity.BLOCK) {
                    hasBlock = true;
                } else if (severity == PolicySeverity.WARN) {
                    hasWarn = true;
                }
            }
        }

        String verdict;
        if (hasBlock) {
            verdict = PolicyVerdict.FAIL;
        } else if (hasPendingApproval) {
            verdict = PolicyVerdict.PENDING_APPROVAL;
        } else if (hasWarn) {
            verdict = PolicyVerdict.WARN;
        } else {
            verdict = PolicyVerdict.PASS;
        }

        return PolicyEngineResult.builder()
                .verdict(verdict)
                .violations(violations)
                .build();
        } catch (RuntimeException ex) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR);
            throw ex;
        } finally {
            span.end();
        }
    }

    private String actionFor(PolicyType type, PolicySeverity severity) {
        if (type == PolicyType.REQUIRE_APPROVAL) {
            return ViolationAction.PENDING_APPROVAL;
        }
        return switch (severity) {
            case BLOCK -> ViolationAction.BLOCKED;
            case WARN -> ViolationAction.WARNED;
            case LOG -> ViolationAction.LOGGED;
        };
    }

    private List<Policy> loadApplicablePolicies(Agent agent, Trace trace) {
        String agentId = agent.getId().toString();
        String tenantId = trace.getTenantId() != null ? trace.getTenantId() : "";
        return policyRepository.findApplicablePolicies(agentId, tenantId);
    }
}
