package dev.ayush.agentlens.trace;

import dev.ayush.agentlens.agent.Agent;
import dev.ayush.agentlens.agent.AgentRepository;
import dev.ayush.agentlens.common.exception.AgentBlockedException;
import dev.ayush.agentlens.common.exception.ResourceNotFoundException;
import dev.ayush.agentlens.common.exception.TracePendingApprovalException;
import dev.ayush.agentlens.common.model.PolicyVerdict;
import dev.ayush.agentlens.common.model.TraceStatus;
import dev.ayush.agentlens.common.util.CostCalculator;
import dev.ayush.agentlens.kafka.AuditEventProducer;
import dev.ayush.agentlens.kafka.TraceEventProducer;
import dev.ayush.agentlens.policy.engine.PolicyEngine;
import dev.ayush.agentlens.policy.engine.PolicyEngineResult;
import dev.ayush.agentlens.trace.dto.AddEventRequest;
import dev.ayush.agentlens.trace.dto.CompleteTraceRequest;
import dev.ayush.agentlens.trace.dto.StartTraceRequest;
import dev.ayush.agentlens.trace.dto.TraceDetailResponse;
import dev.ayush.agentlens.trace.dto.TraceEventResponse;
import dev.ayush.agentlens.trace.dto.TraceResponse;
import dev.ayush.agentlens.trace.dto.TraceSummaryResponse;
import dev.ayush.agentlens.trace.dto.TraceTimelineItemResponse;
import dev.ayush.agentlens.trace.dto.TraceTimelineResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    @Transactional(noRollbackFor = AgentBlockedException.class)
    public TraceResponse startTrace(StartTraceRequest request) {
        Span span = tracer.spanBuilder("trace.start")
                .setAttribute("trace.agent_id", request.getAgentId().toString())
                .setAttribute("trace.model", request.getModel() != null ? request.getModel() : "unknown")
                .startSpan();
        try (var ignored = span.makeCurrent()) {
        Agent agent = agentRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent", request.getAgentId()));

        if (!"ACTIVE".equals(agent.getStatus())) {
            throw new AgentBlockedException("Agent '" + agent.getName() + "' is " + agent.getStatus());
        }

        Trace trace = Trace.builder()
                .agent(agent)
                .traceId(UUID.randomUUID().toString().replace("-", ""))
                .status(TraceStatus.RUNNING)
                .model(request.getModel())
                .promptText(request.getPromptText())
                .temperature(request.getTemperature())
                .tenantId(request.getTenantId())
                .sessionId(request.getSessionId())
                .metadata(request.getMetadata())
                .startedAt(LocalDateTime.now())
                .build();

        trace = traceRepository.save(trace);

        PolicyEngineResult preResult = policyEngine.evaluatePreExecution(trace, agent);
        if (PolicyVerdict.FAIL.equals(preResult.getVerdict())) {
            String reason = firstViolationReason(preResult, "Policy violation");
            trace.setStatus(TraceStatus.BLOCKED);
            trace.setPolicyResult(PolicyVerdict.FAIL);
            trace.setBlockedReason(reason);
            traceRepository.save(trace);
            auditEventProducer.publishTraceBlocked(trace, reason);
            throw new AgentBlockedException("Trace blocked by policy: " + reason);
        }
        if (PolicyVerdict.WARN.equals(preResult.getVerdict())) {
            trace.setPolicyResult(PolicyVerdict.WARN);
            traceRepository.save(trace);
        }

        auditEventProducer.publishTraceStarted(trace);
        meterRegistry.counter("agentlens.trace.started").increment();
        return toTraceResponse(trace);
        } catch (RuntimeException ex) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR);
            throw ex;
        } finally {
            span.end();
        }
    }

    @Transactional(noRollbackFor = {AgentBlockedException.class, TracePendingApprovalException.class})
    public TraceEventResponse addEvent(UUID traceId, AddEventRequest request) {
        Span span = tracer.spanBuilder("trace.event.ingest")
                .setAttribute("trace.id", traceId.toString())
                .setAttribute("trace.event_type", request.getEventType())
                .setAttribute("trace.event_name", request.getEventName())
                .startSpan();
        try (var ignored = span.makeCurrent()) {
        Trace trace = traceRepository.findById(traceId)
                .orElseThrow(() -> new ResourceNotFoundException("Trace", traceId));
        ensureTraceAcceptsEvents(trace);

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
        List<TraceEvent> events = traceEventRepository.findByTraceIdOrderBySequenceNumAsc(traceId);
        PolicyEngineResult eventResult = policyEngine.evaluateEventIngest(trace, events, event, trace.getAgent());

        if (PolicyVerdict.FAIL.equals(eventResult.getVerdict())) {
            String reason = firstViolationReason(eventResult, "Policy violation");
            trace.setStatus(TraceStatus.BLOCKED);
            trace.setPolicyResult(PolicyVerdict.FAIL);
            trace.setBlockedReason(reason);
            traceRepository.save(trace);
            auditEventProducer.publishTraceBlocked(trace, reason);
            meterRegistry.counter("agentlens.trace.blocked").increment();
            throw new AgentBlockedException("Trace blocked by policy: " + reason);
        }

        if (PolicyVerdict.PENDING_APPROVAL.equals(eventResult.getVerdict())) {
            String reason = firstViolationReason(eventResult, "Awaiting human approval");
            trace.setStatus(TraceStatus.PENDING_APPROVAL);
            trace.setPolicyResult(PolicyVerdict.PENDING_APPROVAL);
            trace.setBlockedReason(reason);
            traceRepository.save(trace);
            auditEventProducer.publishTracePendingApproval(trace, reason);
            meterRegistry.counter("agentlens.trace.pending_approval").increment();
            throw new TracePendingApprovalException("Trace is waiting for approval: " + reason);
        }

        if (PolicyVerdict.WARN.equals(eventResult.getVerdict())) {
            trace.setPolicyResult(mergePolicyVerdict(trace.getPolicyResult(), PolicyVerdict.WARN));
            traceRepository.save(trace);
        }

        return toEventResponse(event);
        } catch (RuntimeException ex) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR);
            throw ex;
        } finally {
            span.end();
        }
    }

    @Transactional
    public TraceResponse completeTrace(UUID traceId, CompleteTraceRequest request) {
        Span span = tracer.spanBuilder("trace.complete")
                .setAttribute("trace.id", traceId.toString())
                .startSpan();
        try (var ignored = span.makeCurrent()) {
        Trace trace = traceRepository.findById(traceId)
                .orElseThrow(() -> new ResourceNotFoundException("Trace", traceId));
        ensureTraceCanComplete(trace);

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
        trace.setStatus(TraceStatus.COMPLETED);

        List<TraceEvent> events = traceEventRepository.findByTraceIdOrderBySequenceNumAsc(traceId);
        PolicyEngineResult postResult = policyEngine.evaluatePostCompletion(trace, events, trace.getAgent());
        trace.setPolicyResult(mergePolicyVerdict(trace.getPolicyResult(), postResult.getVerdict()));

        if (PolicyVerdict.FAIL.equals(postResult.getVerdict())) {
            String reason = firstViolationReason(postResult, "Policy violation");
            trace.setBlockedReason(reason);
            trace.setStatus(TraceStatus.BLOCKED);
        }

        trace = traceRepository.save(trace);

        int toolCallCount = (int) events.stream()
                .filter(e -> "TOOL_CALL".equals(e.getEventType()))
                .count();
        traceEventProducer.publishTraceCompleted(trace, trace.getAgent(), toolCallCount);
        auditEventProducer.publishTraceCompleted(trace);
        if (TraceStatus.BLOCKED.equals(trace.getStatus()) && trace.getBlockedReason() != null) {
            auditEventProducer.publishTraceBlocked(trace, trace.getBlockedReason());
            meterRegistry.counter("agentlens.trace.blocked").increment();
        } else {
            meterRegistry.counter("agentlens.trace.completed").increment();
        }
        if (trace.getLatencyMs() != null) {
            meterRegistry.timer("agentlens.trace.latency").record(trace.getLatencyMs(), TimeUnit.MILLISECONDS);
        }

        return toTraceResponse(trace);
        } catch (RuntimeException ex) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR);
            throw ex;
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public TraceDetailResponse getTrace(UUID id) {
        Trace trace = traceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trace", id));

        List<TraceEventResponse> events = traceEventRepository.findByTraceIdOrderBySequenceNumAsc(id)
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
    public TraceTimelineResponse getTraceTimeline(UUID id) {
        Trace trace = traceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trace", id));

        List<TraceTimelineItemResponse> items = traceEventRepository.findByTraceIdOrderBySequenceNumAsc(id)
                .stream()
                .map(event -> TraceTimelineItemResponse.builder()
                        .eventId(event.getId())
                        .eventType(event.getEventType())
                        .eventName(event.getEventName())
                        .status(event.getStatus())
                        .timestamp(event.getTimestamp())
                        .relativeStartMs(Duration.between(trace.getStartedAt(), event.getTimestamp()).toMillis())
                        .durationMs(event.getDurationMs())
                        .errorMessage(event.getErrorMessage())
                        .inputData(event.getInputData())
                        .outputData(event.getOutputData())
                        .build())
                .toList();

        LocalDateTime end = trace.getCompletedAt() != null ? trace.getCompletedAt() : LocalDateTime.now();

        return TraceTimelineResponse.builder()
                .traceId(trace.getId())
                .agentId(trace.getAgent().getId())
                .agentName(trace.getAgent().getName())
                .status(trace.getStatus())
                .policyResult(trace.getPolicyResult())
                .startedAt(trace.getStartedAt())
                .completedAt(trace.getCompletedAt())
                .totalDurationMs(Duration.between(trace.getStartedAt(), end).toMillis())
                .items(items)
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

    private void ensureTraceAcceptsEvents(Trace trace) {
        if (TraceStatus.PENDING_APPROVAL.equals(trace.getStatus())) {
            throw new TracePendingApprovalException("Trace is waiting for human approval");
        }
        if (TraceStatus.BLOCKED.equals(trace.getStatus())) {
            throw new AgentBlockedException("Trace is blocked and cannot accept new events");
        }
        if (TraceStatus.COMPLETED.equals(trace.getStatus()) || TraceStatus.FAILED.equals(trace.getStatus())) {
            throw new IllegalStateException("Trace is already finalized");
        }
    }

    private void ensureTraceCanComplete(Trace trace) {
        if (TraceStatus.PENDING_APPROVAL.equals(trace.getStatus())) {
            throw new TracePendingApprovalException("Trace is waiting for human approval");
        }
        if (TraceStatus.BLOCKED.equals(trace.getStatus())) {
            throw new AgentBlockedException("Trace is blocked and cannot be completed");
        }
        if (TraceStatus.COMPLETED.equals(trace.getStatus()) || TraceStatus.FAILED.equals(trace.getStatus())) {
            throw new IllegalStateException("Trace is already finalized");
        }
    }

    private String firstViolationReason(PolicyEngineResult result, String fallback) {
        return result.getViolations().stream()
                .findFirst()
                .map(v -> v.getPolicy().getName() + ": " + v.getDetails())
                .orElse(fallback);
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
}
