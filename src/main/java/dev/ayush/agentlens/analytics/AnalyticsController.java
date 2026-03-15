package dev.ayush.agentlens.analytics;

import dev.ayush.agentlens.analytics.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public SummaryResponse getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) String tenantId) {
        return analyticsService.getSummary(filters(startDate, endDate, agentId, tenantId));
    }

    @GetMapping("/cost")
    public CostBreakdownResponse getCostBreakdown(
            @RequestParam(defaultValue = "model") String groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) String tenantId) {
        return analyticsService.getCostBreakdown(filters(startDate, endDate, agentId, tenantId), groupBy);
    }

    @GetMapping("/latency")
    public LatencyResponse getLatencyPercentiles(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) String tenantId) {
        return analyticsService.getLatencyPercentiles(filters(startDate, endDate, agentId, tenantId));
    }

    @GetMapping("/violations")
    public ViolationTrendsResponse getViolationTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) String tenantId) {
        return analyticsService.getViolationTrends(filters(startDate, endDate, agentId, tenantId));
    }

    @GetMapping("/top-agents")
    public TopAgentsResponse getTopAgents(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) String tenantId) {
        return analyticsService.getTopAgents(filters(startDate, endDate, agentId, tenantId), limit);
    }

    private AnalyticsFilters filters(Instant startDate, Instant endDate, UUID agentId, String tenantId) {
        return AnalyticsFilters.builder()
                .startDate(startDate)
                .endDate(endDate)
                .agentId(agentId)
                .tenantId(tenantId)
                .build();
    }
}
