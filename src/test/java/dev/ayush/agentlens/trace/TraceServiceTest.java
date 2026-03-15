package dev.ayush.agentlens.trace;

import dev.ayush.agentlens.agent.Agent;
import dev.ayush.agentlens.agent.AgentRepository;
import dev.ayush.agentlens.common.exception.TracePendingApprovalException;
import dev.ayush.agentlens.common.model.PolicyVerdict;
import dev.ayush.agentlens.common.model.TraceStatus;
import dev.ayush.agentlens.common.util.CostCalculator;
import dev.ayush.agentlens.kafka.AuditEventProducer;
import dev.ayush.agentlens.kafka.TraceEventProducer;
import dev.ayush.agentlens.policy.Policy;
import dev.ayush.agentlens.policy.PolicyViolation;
import dev.ayush.agentlens.policy.engine.PolicyEngine;
import dev.ayush.agentlens.policy.engine.PolicyEngineResult;
import dev.ayush.agentlens.trace.dto.AddEventRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceServiceTest {

    @Mock
    private TraceRepository traceRepository;
    @Mock
    private TraceEventRepository traceEventRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private PolicyEngine policyEngine;
    @Mock
    private TraceEventProducer traceEventProducer;
    @Mock
    private AuditEventProducer auditEventProducer;

    private TraceService traceService;

    @BeforeEach
    void setUp() {
        traceService = new TraceService(
                traceRepository,
                traceEventRepository,
                agentRepository,
                new CostCalculator(),
                policyEngine,
                traceEventProducer,
                auditEventProducer,
                OpenTelemetry.noop().getTracer("test"),
                new SimpleMeterRegistry()
        );
    }

    @Test
    void addEventMovesTraceToPendingApprovalAndStopsExecution() {
        Agent agent = Agent.builder().id(UUID.randomUUID()).name("agent").status("ACTIVE").build();
        Trace trace = Trace.builder()
                .id(UUID.randomUUID())
                .agent(agent)
                .status(TraceStatus.RUNNING)
                .traceId("trace-id")
                .startedAt(LocalDateTime.now())
                .build();
        Policy policy = Policy.builder().name("Approval").policyType("REQUIRE_APPROVAL").severity("BLOCK").build();
        PolicyViolation violation = PolicyViolation.builder()
                .trace(trace)
                .policy(policy)
                .violationType("REQUIRE_APPROVAL")
                .severity("BLOCK")
                .details(Map.of("tool", "execShell"))
                .actionTaken("PENDING_APPROVAL")
                .build();

        when(traceRepository.findById(trace.getId())).thenReturn(Optional.of(trace));
        when(traceEventRepository.countByTraceId(trace.getId())).thenReturn(0L);
        when(traceEventRepository.save(any(TraceEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(traceEventRepository.findByTraceIdOrderBySequenceNumAsc(trace.getId())).thenAnswer(invocation -> List.of(invocation.getArgument(0) != null ? TraceEvent.builder()
                .trace(trace)
                .eventType("TOOL_CALL")
                .eventName("execShell")
                .sequenceNum(1)
                .timestamp(LocalDateTime.now())
                .build() : null));
        when(policyEngine.evaluateEventIngest(any(), any(), any(), any()))
                .thenReturn(PolicyEngineResult.builder()
                        .verdict(PolicyVerdict.PENDING_APPROVAL)
                        .violations(List.of(violation))
                        .build());
        when(traceRepository.save(any(Trace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> traceService.addEvent(trace.getId(), new AddEventRequest(
                "TOOL_CALL",
                "execShell",
                Map.of(),
                Map.of(),
                "SUCCESS",
                50L,
                null
        ))).isInstanceOf(TracePendingApprovalException.class);

        assertThat(trace.getStatus()).isEqualTo(TraceStatus.PENDING_APPROVAL);
        assertThat(trace.getPolicyResult()).isEqualTo(PolicyVerdict.PENDING_APPROVAL);
        verify(auditEventProducer).publishTracePendingApproval(any(Trace.class), any(String.class));
    }
}
