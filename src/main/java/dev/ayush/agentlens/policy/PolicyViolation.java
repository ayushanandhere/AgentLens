package dev.ayush.agentlens.policy;

import dev.ayush.agentlens.trace.Trace;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "policy_violations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trace_id", nullable = false)
    private Trace trace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "violation_type", nullable = false, length = 30)
    private String violationType;

    @Column(nullable = false, length = 10)
    private String severity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "action_taken", length = 20)
    private String actionTaken;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
