package dev.ayush.agentlens.trace;

import dev.ayush.agentlens.agent.Agent;
import dev.ayush.agentlens.agent.AgentRepository;
import dev.ayush.agentlens.common.exception.AgentBlockedException;
import dev.ayush.agentlens.common.exception.ResourceNotFoundException;
import dev.ayush.agentlens.common.util.CostCalculator;
import dev.ayush.agentlens.kafka.AuditEventProducer;
import dev.ayush.agentlens.kafka.TraceEventProducer;
import dev.ayush.agentlens.policy.PolicyViolation;
import dev.ayush.agentlens.policy.engine.PolicyEngine;
import dev.ayush.agentlens.policy.engine.PolicyEngineResult;
import dev.ayush.agentlens.trace.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TraceService {

    private final TraceRepository traceRepository;
    private final TraceEventRepository traceEventRepository;
    private final AgentRepository agentRepository;
    private final CostCalculator costCalculator;
    private final PolicyEngine policyEngine;
    private final TraceEventProducer traceEventProducer;
    private final AuditEventProducer auditEventProducer;

    @Transactional
    public TraceResponse startTrace(StartTraceRequest request) {
        Agent agent = agentRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent", request.getAgentId()));

        if (!"ACTIVE".equals(agent.getStatus())) {
            throw new AgentBlockedException("Agent '" + agent.getName() + "' is " + agent.getStatus());
        }

        Trace trace = Trace.builder()
                .agent(agent)
                .traceId(UUID.randomUUID().toString().replace("-", ""))
                .status("RUNNING")
                .model(request.getModel())
                .promptText(request.getPromptText())
                .temperature(request.getTemperature())
                .tenantId(request.getTenantId())
                .sessionId(request.getSessionId())
                .metadata(request.getMetadata())
                .startedAt(LocalDateTime.now())
                .build();

        trace = traceRepository.save(trace);

        // Pre-execution policy evaluation
        PolicyEngineResult preResult = policyEngine.evaluatePreExecution(trace, agent);
        if ("FAIL".equals(preResult.getVerdict())) {
            trace.setStatus("BLOCKED");
            trace.setPolicyResult("FAIL");
            String reason = preResult.getViolations().stream()
                    .findFirst()
                    .map(v -> v.getViolationType() + ": " + v.getDetails())
                    .orElse("Policy violation");
            trace.setBlockedReason(reason);
            traceRepository.save(trace);
            auditEventProducer.publishTraceBlocked(trace, reason);
            throw new AgentBlockedException("Trace blocked by policy: " + reason);
        } else if ("WARN".equals(preResult.getVerdict())) {
            trace.setPolicyResult("WARN");
            traceRepository.save(trace);
        }

        auditEventProducer.publishTraceStarted(trace);
        return toTraceResponse(trace);
    }

    @Transactional
    public TraceEventResponse addEvent(UUID traceId, AddEventRequest request) {
        Trace trace = traceRepository.findById(traceId)
                .orElseThrow(() -> new ResourceNotFoundException("Trace", traceId));

        long count = traceEventRepository.countByTraceId(traceId);

        TraceEvent event = TraceEvent.builder()
                .trace(trace)
                .eventType(request.getEventType())
                .eventName(request.getEventName())
                .inputData(request.getInputData())
                .outputData(request.getOutputData())
                .status(request.getStatus() != null ? request.getStatus() : "SUCCESS")
                .durationMs(request.getDurationMs())
                .errorMessage(request.getErrorMessage())
                .sequenceNum((int) count + 1)
                .build();

        event = traceEventRepository.save(event);
        return toEventResponse(event);
    }

    @Transactional
    public TraceResponse completeTrace(UUID traceId, CompleteTraceRequest request) {
        Trace trace = traceRepository.findById(traceId)
                .orElseThrow(() -> new ResourceNotFoundException("Trace", traceId));

        trace.setResponseText(request.getResponseText());
        trace.setInputTokens(request.getInputTokens());
        trace.setOutputTokens(request.getOutputTokens());
        trace.setTotalTokens(request.getInputTokens() + request.getOutputTokens());
        trace.setEstimatedCost(costCalculator.calculateCost(
                trace.getModel(), request.getInputTokens(), request.getOutputTokens()));
        trace.setCompletedAt(LocalDateTime.now());
        trace.setLatencyMs(Duration.between(trace.getStartedAt(), trace.getCompletedAt()).toMillis());
        trace.setTtftMs(request.getTtftMs());
        trace.setGroundingScore(request.getGroundingScore());
        trace.setStatus("COMPLETED");

        // Post-completion policy evaluation
        List<TraceEvent> events = traceEventRepository.findByTraceIdOrderBySequenceNumAsc(traceId);
        PolicyEngineResult postResult = policyEngine.evaluatePostCompletion(trace, events, trace.getAgent());
        trace.setPolicyResult(postResult.getVerdict());

        if ("FAIL".equals(postResult.getVerdict())) {
            String reason = postResult.getViolations().stream()
                    .filter(v -> "BLOCK".equals(v.getSeverity()))
                    .findFirst()
                    .map(v -> v.getViolationType() + ": " + v.getDetails())
                    .orElse("Policy violation");
            trace.setBlockedReason(reason);
            trace.setStatus("BLOCKED");
        }

        trace = traceRepository.save(trace);

        // Publish Kafka events
        int toolCallCount = (int) events.stream()
                .filter(e -> "TOOL_CALL".equals(e.getEventType()))
                .count();
        traceEventProducer.publishTraceCompleted(trace, trace.getAgent(), toolCallCount);
        auditEventProducer.publishTraceCompleted(trace);

        return toTraceResponse(trace);
    }

    @Transactional(readOnly = true)
    public TraceDetailResponse getTrace(UUID id) {
        Trace trace = traceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trace", id));

        List<TraceEventResponse> events = traceEventRepository
                .findByTraceIdOrderBySequenceNumAsc(id)
                .stream()
                .map(this::toEventResponse)
                .toList();

        return TraceDetailResponse.builder()
                .id(trace.getId())
                .agentId(trace.getAgent().getId())
                .agentName(trace.getAgent().getName())
                .traceId(trace.getTraceId())
                .status(trace.getStatus())
                .model(trace.getModel())
                .promptText(trace.getPromptText())
                .responseText(trace.getResponseText())
                .temperature(trace.getTemperature())
                .inputTokens(trace.getInputTokens())
                .outputTokens(trace.getOutputTokens())
                .totalTokens(trace.getTotalTokens())
                .estimatedCost(trace.getEstimatedCost())
                .startedAt(trace.getStartedAt())
                .completedAt(trace.getCompletedAt())
                .latencyMs(trace.getLatencyMs())
                .ttftMs(trace.getTtftMs())
                .groundingScore(trace.getGroundingScore())
                .policyResult(trace.getPolicyResult())
                .blockedReason(trace.getBlockedReason())
                .tenantId(trace.getTenantId())
                .sessionId(trace.getSessionId())
                .metadata(trace.getMetadata())
                .createdAt(trace.getCreatedAt())
                .events(events)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<TraceSummaryResponse> listTraces(UUID agentId, String status,
                                                  String policyResult, String tenantId,
                                                  LocalDateTime startDate, LocalDateTime endDate,
                                                  Pageable pageable) {
        Specification<Trace> spec = TraceSpecification.withFilters(
                agentId, status, policyResult, tenantId, startDate, endDate);
        return traceRepository.findAll(spec, pageable).map(this::toSummaryResponse);
    }

    private TraceResponse toTraceResponse(Trace trace) {
        return TraceResponse.builder()
                .id(trace.getId())
                .agentId(trace.getAgent().getId())
                .traceId(trace.getTraceId())
                .status(trace.getStatus())
                .model(trace.getModel())
                .promptText(trace.getPromptText())
                .responseText(trace.getResponseText())
                .temperature(trace.getTemperature())
                .inputTokens(trace.getInputTokens())
                .outputTokens(trace.getOutputTokens())
                .totalTokens(trace.getTotalTokens())
                .estimatedCost(trace.getEstimatedCost())
                .startedAt(trace.getStartedAt())
                .completedAt(trace.getCompletedAt())
                .latencyMs(trace.getLatencyMs())
                .ttftMs(trace.getTtftMs())
                .groundingScore(trace.getGroundingScore())
                .policyResult(trace.getPolicyResult())
                .blockedReason(trace.getBlockedReason())
                .tenantId(trace.getTenantId())
                .sessionId(trace.getSessionId())
                .metadata(trace.getMetadata())
                .createdAt(trace.getCreatedAt())
                .build();
    }

    private TraceSummaryResponse toSummaryResponse(Trace trace) {
        return TraceSummaryResponse.builder()
                .id(trace.getId())
                .agentId(trace.getAgent().getId())
                .agentName(trace.getAgent().getName())
                .model(trace.getModel())
                .status(trace.getStatus())
                .policyResult(trace.getPolicyResult())
                .totalTokens(trace.getTotalTokens())
                .estimatedCost(trace.getEstimatedCost())
                .latencyMs(trace.getLatencyMs())
                .groundingScore(trace.getGroundingScore())
                .startedAt(trace.getStartedAt())
                .build();
    }

    private TraceEventResponse toEventResponse(TraceEvent event) {
        return TraceEventResponse.builder()
                .id(event.getId())
                .traceId(event.getTrace().getId())
                .eventType(event.getEventType())
                .eventName(event.getEventName())
                .inputData(event.getInputData())
                .outputData(event.getOutputData())
                .status(event.getStatus())
                .durationMs(event.getDurationMs())
                .errorMessage(event.getErrorMessage())
                .sequenceNum(event.getSequenceNum())
                .timestamp(event.getTimestamp())
                .build();
    }
}
