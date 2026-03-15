package dev.ayush.agentlens.trace;

import dev.ayush.agentlens.trace.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/traces")
@RequiredArgsConstructor
public class TraceController {

    private final TraceService traceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TraceResponse startTrace(@Valid @RequestBody StartTraceRequest request) {
        return traceService.startTrace(request);
    }

    @PostMapping("/{id}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public TraceEventResponse addEvent(@PathVariable UUID id,
                                       @Valid @RequestBody AddEventRequest request) {
        return traceService.addEvent(id, request);
    }

    @PutMapping("/{id}/complete")
    public TraceResponse completeTrace(@PathVariable UUID id,
                                       @Valid @RequestBody CompleteTraceRequest request) {
        return traceService.completeTrace(id, request);
    }

    @GetMapping
    public Page<TraceSummaryResponse> listTraces(
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String policyResult,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        return traceService.listTraces(agentId, status, policyResult, tenantId, startDate, endDate, pageable);
    }

    @GetMapping("/{id}")
    public TraceDetailResponse getTrace(@PathVariable UUID id) {
        return traceService.getTrace(id);
    }

    @GetMapping("/{id}/timeline")
    public TraceTimelineResponse getTraceTimeline(@PathVariable UUID id) {
        return traceService.getTraceTimeline(id);
    }
}
