package dev.ayush.agentlens.trace;

import dev.ayush.agentlens.agent.Agent;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "traces")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 100)
    private String model;

    @Column(name = "prompt_text")
    private String promptText;

    @Column(name = "response_text")
    private String responseText;

    @Column(precision = 3, scale = 2)
    private BigDecimal temperature;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "estimated_cost", precision = 10, scale = 6)
    private BigDecimal estimatedCost;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "ttft_ms")
    private Long ttftMs;

    @Column(name = "grounding_score", precision = 3, scale = 2)
    private BigDecimal groundingScore;

    @Column(name = "policy_result", length = 20)
    private String policyResult;

    @Column(name = "blocked_reason")
    private String blockedReason;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
