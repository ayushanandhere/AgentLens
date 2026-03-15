package dev.ayush.agentlens.analytics;

import dev.ayush.agentlens.analytics.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    @PersistenceContext
    private EntityManager entityManager;

    public SummaryResponse getSummary(AnalyticsFilters filters) {
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder("""
                SELECT
                  COUNT(*) as total_traces,
                  COUNT(*) FILTER (WHERE status = 'COMPLETED') as completed_traces,
                  COUNT(*) FILTER (WHERE status = 'FAILED') as failed_traces,
                  COUNT(*) FILTER (WHERE status = 'BLOCKED') as blocked_traces,
                  AVG(latency_ms) FILTER (WHERE latency_ms IS NOT NULL) as avg_latency_ms,
                  AVG(estimated_cost) FILTER (WHERE estimated_cost IS NOT NULL) as avg_cost_usd,
                  SUM(estimated_cost) FILTER (WHERE estimated_cost IS NOT NULL) as total_cost_usd,
                  AVG(grounding_score) FILTER (WHERE grounding_score IS NOT NULL) as avg_grounding_score,
                  COUNT(*) FILTER (WHERE policy_result IS NOT NULL AND policy_result <> 'PASS') * 100.0 / NULLIF(COUNT(*), 0) as violation_rate_percent
                FROM traces
                """);

        appendTraceFilters(sql, "", filters, params, false);

        Object[] row = (Object[]) createQuery(sql.toString(), params).getSingleResult();
        return SummaryResponse.builder()
                .totalTraces(longValue(row[0]))
                .completedTraces(longValue(row[1]))
                .failedTraces(longValue(row[2]))
                .blockedTraces(longValue(row[3]))
                .avgLatencyMs(doubleValue(row[4]))
                .avgCostUsd(bigDecimalValue(row[5]))
                .totalCostUsd(bigDecimalValue(row[6]))
                .avgGroundingScore(doubleValue(row[7]))
                .violationRatePercent(doubleValue(row[8]))
                .build();
    }

    public CostBreakdownResponse getCostBreakdown(AnalyticsFilters filters, String groupBy) {
        String normalizedGroupBy = (groupBy == null || groupBy.isBlank()) ? "model" : groupBy.toLowerCase();
        return switch (normalizedGroupBy) {
            case "agent" -> getCostBreakdownByAgent(filters);
            case "model" -> getCostBreakdownByModel(filters);
            default -> throw new IllegalArgumentException("groupBy must be one of: model, agent");
        };
    }

    public LatencyResponse getLatencyPercentiles(AnalyticsFilters filters) {
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder("""
                SELECT
                  PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY latency_ms) as p50,
                  PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY latency_ms) as p95,
                  PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY latency_ms) as p99,
                  AVG(latency_ms) as avg,
                  MIN(latency_ms) as min,
                  MAX(latency_ms) as max
                FROM traces
                WHERE latency_ms IS NOT NULL
                """);

        appendTraceFilters(sql, "", filters, params, true);

        Object[] row = (Object[]) createQuery(sql.toString(), params).getSingleResult();
        return LatencyResponse.builder()
                .p50(doubleValue(row[0]))
                .p95(doubleValue(row[1]))
                .p99(doubleValue(row[2]))
                .avg(doubleValue(row[3]))
                .min(doubleValue(row[4]))
                .max(doubleValue(row[5]))
                .build();
    }

    public ViolationTrendsResponse getViolationTrends(AnalyticsFilters filters) {
        return ViolationTrendsResponse.builder()
                .byType(getViolationCountsByType(filters))
                .bySeverity(getViolationCountsBySeverity(filters))
                .byDay(getViolationCountsByDay(filters))
                .build();
    }

    public TopAgentsResponse getTopAgents(AnalyticsFilters filters, int limit) {
        int boundedLimit = Math.max(1, limit);
        return TopAgentsResponse.builder()
                .mostActive(getMostActiveAgents(filters, boundedLimit))
                .mostExpensive(getMostExpensiveAgents(filters, boundedLimit))
                .mostViolations(getMostViolatedAgents(filters, boundedLimit))
                .build();
    }

    private CostBreakdownResponse getCostBreakdownByModel(AnalyticsFilters filters) {
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder("""
                SELECT model, SUM(estimated_cost) as total_cost, SUM(total_tokens) as total_tokens, COUNT(*) as trace_count
                FROM traces
                WHERE estimated_cost IS NOT NULL
                """);

        appendTraceFilters(sql, "", filters, params, true);
        sql.append(" GROUP BY model ORDER BY total_cost DESC");

        List<CostBreakdownEntry> entries = new ArrayList<>();
        for (Object[] row : resultList(sql.toString(), params)) {
            String model = row[0] != null ? row[0].toString() : "unknown";
            entries.add(CostBreakdownEntry.builder()
                    .key(model)
                    .label(model)
                    .totalCost(bigDecimalValue(row[1]))
                    .totalTokens(longValue(row[2]))
                    .traceCount(longValue(row[3]))
                    .build());
        }

        return CostBreakdownResponse.builder().entries(entries).build();
    }

    private CostBreakdownResponse getCostBreakdownByAgent(AnalyticsFilters filters) {
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder("""
                SELECT t.agent_id, a.name as agent_name, SUM(t.estimated_cost) as total_cost,
                       SUM(t.total_tokens) as total_tokens, COUNT(*) as trace_count
                FROM traces t
                JOIN agents a ON t.agent_id = a.id
                WHERE t.estimated_cost IS NOT NULL
                """);

        appendTraceFilters(sql, "t", filters, params, true);
        sql.append(" GROUP BY t.agent_id, a.name ORDER BY total_cost DESC");

        List<CostBreakdownEntry> entries = new ArrayList<>();
        for (Object[] row : resultList(sql.toString(), params)) {
            String key = row[0].toString();
            entries.add(CostBreakdownEntry.builder()
                    .key(key)
                    .label(stringValue(row[1]))
                    .totalCost(bigDecimalValue(row[2]))
                    .totalTokens(longValue(row[3]))
                    .traceCount(longValue(row[4]))
                    .build());
        }

        return CostBreakdownResponse.builder().entries(entries).build();
    }

    private List<ViolationTypeCount> getViolationCountsByType(AnalyticsFilters filters) {
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder("""
                SELECT pv.violation_type, COUNT(*) as count
                FROM policy_violations pv
                JOIN traces t ON pv.trace_id = t.id
                """);

        appendTraceFilters(sql, "t", filters, params, false);
        sql.append(" GROUP BY pv.violation_type ORDER BY count DESC");

        List<ViolationTypeCount> counts = new ArrayList<>();
        for (Object[] row : resultList(sql.toString(), params)) {
            counts.add(ViolationTypeCount.builder()
                    .type(stringValue(row[0]))
                    .count(longValue(row[1]))
                    .build());
        }
        return counts;
    }

    private List<ViolationSeverityCount> getViolationCountsBySeverity(AnalyticsFilters filters) {
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder("""
                SELECT pv.severity, COUNT(*) as count
                FROM policy_violations pv
                JOIN traces t ON pv.trace_id = t.id
                """);

        appendTraceFilters(sql, "t", filters, params, false);
        sql.append(" GROUP BY pv.severity ORDER BY count DESC");

        List<ViolationSeverityCount> counts = new ArrayList<>();
        for (Object[] row : resultList(sql.toString(), params)) {
            counts.add(ViolationSeverityCount.builder()
                    .severity(stringValue(row[0]))
                    .count(longValue(row[1]))
                    .build());
        }
        return counts;
    }

    private List<ViolationDayCount> getViolationCountsByDay(AnalyticsFilters filters) {
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder("""
                SELECT DATE(pv.created_at) as day, COUNT(*) as count
                FROM policy_violations pv
                JOIN traces t ON pv.trace_id = t.id
                """);

        appendTraceFilters(sql, "t", filters, params, false);
        sql.append(" GROUP BY DATE(pv.created_at) ORDER BY day");

        List<ViolationDayCount> counts = new ArrayList<>();
        for (Object[] row : resultList(sql.toString(), params)) {
            counts.add(ViolationDayCount.builder()
                    .day(localDateValue(row[0]))
                    .count(longValue(row[1]))
                    .build());
        }
        return counts;
    }

    private List<TopAgentEntry> getMostActiveAgents(AnalyticsFilters filters, int limit) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("limit", limit);
        StringBuilder sql = new StringBuilder("""
                SELECT a.id, a.name, COUNT(*) as trace_count
                FROM traces t
                JOIN agents a ON t.agent_id = a.id
                """);

        appendTraceFilters(sql, "t", filters, params, false);
        sql.append(" GROUP BY a.id, a.name ORDER BY trace_count DESC LIMIT :limit");
        return mapAgentMetrics(sql.toString(), params, false);
    }

    private List<TopAgentEntry> getMostExpensiveAgents(AnalyticsFilters filters, int limit) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("limit", limit);
        StringBuilder sql = new StringBuilder("""
                SELECT a.id, a.name, SUM(t.estimated_cost) as total_cost
                FROM traces t
                JOIN agents a ON t.agent_id = a.id
                WHERE t.estimated_cost IS NOT NULL
                """);

        appendTraceFilters(sql, "t", filters, params, true);
        sql.append(" GROUP BY a.id, a.name ORDER BY total_cost DESC LIMIT :limit");
        return mapAgentMetrics(sql.toString(), params, true);
    }

    private List<TopAgentEntry> getMostViolatedAgents(AnalyticsFilters filters, int limit) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("limit", limit);
        StringBuilder sql = new StringBuilder("""
                SELECT a.id, a.name, COUNT(pv.id) as violation_count
                FROM policy_violations pv
                JOIN traces t ON pv.trace_id = t.id
                JOIN agents a ON t.agent_id = a.id
                """);

        appendTraceFilters(sql, "t", filters, params, false);
        sql.append(" GROUP BY a.id, a.name ORDER BY violation_count DESC LIMIT :limit");
        return mapAgentMetrics(sql.toString(), params, false);
    }

    private List<TopAgentEntry> mapAgentMetrics(String sql, Map<String, Object> params, boolean decimalMetric) {
        List<TopAgentEntry> entries = new ArrayList<>();
        for (Object[] row : resultList(sql, params)) {
            entries.add(TopAgentEntry.builder()
                    .agentId(uuidValue(row[0]))
                    .agentName(stringValue(row[1]))
                    .value(decimalMetric ? bigDecimalValue(row[2]) : longValue(row[2]))
                    .build());
        }
        return entries;
    }

    private void appendTraceFilters(StringBuilder sql, String alias, AnalyticsFilters filters,
                                    Map<String, Object> params, boolean hasWhere) {
        if (filters == null) {
            return;
        }

        boolean whereAdded = hasWhere;
        String prefix = (alias == null || alias.isBlank()) ? "" : alias + ".";

        if (filters.getStartDate() != null) {
            sql.append(whereAdded ? " AND " : " WHERE ")
                    .append(prefix)
                    .append("started_at >= :startDate");
            params.put("startDate", toTimestamp(filters.getStartDate()));
            whereAdded = true;
        }
        if (filters.getEndDate() != null) {
            sql.append(whereAdded ? " AND " : " WHERE ")
                    .append(prefix)
                    .append("started_at <= :endDate");
            params.put("endDate", toTimestamp(filters.getEndDate()));
            whereAdded = true;
        }
        if (filters.getAgentId() != null) {
            sql.append(whereAdded ? " AND " : " WHERE ")
                    .append(prefix)
                    .append("agent_id = :agentId");
            params.put("agentId", filters.getAgentId());
            whereAdded = true;
        }
        if (filters.getTenantId() != null && !filters.getTenantId().isBlank()) {
            sql.append(whereAdded ? " AND " : " WHERE ")
                    .append(prefix)
                    .append("tenant_id = :tenantId");
            params.put("tenantId", filters.getTenantId());
        }
    }

    private Query createQuery(String sql, Map<String, Object> params) {
        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return query;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> resultList(String sql, Map<String, Object> params) {
        return createQuery(sql, params).getResultList();
    }

    private Timestamp toTimestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    private Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Double doubleValue(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private BigDecimal bigDecimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private String stringValue(Object value) {
        return value != null ? value.toString() : "";
    }

    private UUID uuidValue(Object value) {
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }

    private LocalDate localDateValue(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }
}
