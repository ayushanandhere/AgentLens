package dev.ayush.agentlens.policy.engine;

import dev.ayush.agentlens.agent.Agent;
import dev.ayush.agentlens.policy.Policy;
import dev.ayush.agentlens.policy.PolicyRepository;
import dev.ayush.agentlens.kafka.AuditEventProducer;
import dev.ayush.agentlens.kafka.PolicyViolationProducer;
import dev.ayush.agentlens.policy.PolicyViolation;
import dev.ayush.agentlens.policy.PolicyViolationRepository;
import dev.ayush.agentlens.trace.Trace;
import dev.ayush.agentlens.trace.TraceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyEngine {

    private static final Set<PolicyType> PRE_EXECUTION_TYPES = Set.of(PolicyType.RATE_LIMIT);
    private static final Set<PolicyType> POST_COMPLETION_TYPES = Set.of(
            PolicyType.TOKEN_BUDGET, PolicyType.COST_BUDGET, PolicyType.TOOL_BLOCK, PolicyType.PII_CHECK);

    private final PolicyRepository policyRepository;
    private final PolicyViolationRepository violationRepository;
    private final PolicyEvaluatorRegistry evaluatorRegistry;
    private final PolicyViolationProducer policyViolationProducer;
    private final AuditEventProducer auditEventProducer;

    public PolicyEngineResult evaluatePreExecution(Trace trace, Agent agent) {
        TraceContext context = TraceContext.builder()
                .trace(trace)
                .events(Collections.emptyList())
                .agent(agent)
                .build();

        List<Policy> policies = loadApplicablePolicies(agent, trace);
        List<Policy> preExecPolicies = policies.stream()
                .filter(p -> PRE_EXECUTION_TYPES.contains(PolicyType.valueOf(p.getPolicyType())))
                .toList();

        return evaluatePolicies(preExecPolicies, context, trace);
    }

    public PolicyEngineResult evaluatePostCompletion(Trace trace, List<TraceEvent> events, Agent agent) {
        TraceContext context = TraceContext.builder()
                .trace(trace)
                .events(events)
                .agent(agent)
                .build();

        List<Policy> policies = loadApplicablePolicies(agent, trace);
        List<Policy> postPolicies = policies.stream()
                .filter(p -> POST_COMPLETION_TYPES.contains(PolicyType.valueOf(p.getPolicyType())))
                .toList();

        return evaluatePolicies(postPolicies, context, trace);
    }

    private PolicyEngineResult evaluatePolicies(List<Policy> policies, TraceContext context, Trace trace) {
        List<PolicyViolation> violations = new ArrayList<>();
        boolean hasBlock = false;
        boolean hasWarn = false;

        for (Policy policy : policies) {
            PolicyType type = PolicyType.valueOf(policy.getPolicyType());
            PolicyEvaluator evaluator = evaluatorRegistry.getEvaluator(type);
            PolicyResult result = evaluator.evaluate(policy.getConfig(), context);

            if (result.violated()) {
                PolicySeverity severity = PolicySeverity.valueOf(policy.getSeverity());
                log.warn("Policy violation: {} [{}] on trace {}", policy.getName(), severity, trace.getId());

                PolicyViolation violation = PolicyViolation.builder()
                        .trace(trace)
                        .policy(policy)
                        .violationType(policy.getPolicyType())
                        .severity(policy.getSeverity())
                        .details(result.details())
                        .actionTaken(severity.name())
                        .build();
                violation = violationRepository.save(violation);
                violations.add(violation);

                policyViolationProducer.publishViolation(violation, policy, trace);
                auditEventProducer.publishPolicyViolated(violation, trace);

                if (severity == PolicySeverity.BLOCK) {
                    hasBlock = true;
                } else if (severity == PolicySeverity.WARN) {
                    hasWarn = true;
                }
            }
        }

        String verdict;
        if (hasBlock) {
            verdict = "FAIL";
        } else if (hasWarn) {
            verdict = "WARN";
        } else {
            verdict = "PASS";
        }

        return PolicyEngineResult.builder()
                .verdict(verdict)
                .violations(violations)
                .build();
    }

    private List<Policy> loadApplicablePolicies(Agent agent, Trace trace) {
        String agentId = agent.getId().toString();
        String tenantId = trace.getTenantId() != null ? trace.getTenantId() : "";
        return policyRepository.findApplicablePolicies(agentId, tenantId);
    }
}
