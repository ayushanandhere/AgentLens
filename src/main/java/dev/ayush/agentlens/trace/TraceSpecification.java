package dev.ayush.agentlens.trace;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TraceSpecification {

    private TraceSpecification() {}

    public static Specification<Trace> withFilters(UUID agentId, String status,
                                                    String policyResult, String tenantId,
                                                    LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Fetch agent eagerly to avoid N+1 (skip for count queries)
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("agent", JoinType.LEFT);
            }

            if (agentId != null) {
                predicates.add(cb.equal(root.get("agent").get("id"), agentId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (policyResult != null) {
                predicates.add(cb.equal(root.get("policyResult"), policyResult));
            }
            if (tenantId != null) {
                predicates.add(cb.equal(root.get("tenantId"), tenantId));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startedAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
