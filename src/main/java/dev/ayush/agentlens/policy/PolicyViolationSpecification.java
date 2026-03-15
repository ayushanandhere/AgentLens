package dev.ayush.agentlens.policy;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PolicyViolationSpecification {

    private PolicyViolationSpecification() {
    }

    public static Specification<PolicyViolation> withFilters(UUID traceId, UUID policyId, String severity, String actionTaken) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("trace", JoinType.LEFT);
                root.fetch("policy", JoinType.LEFT);
                query.distinct(true);
            }

            if (traceId != null) {
                predicates.add(cb.equal(root.get("trace").get("id"), traceId));
            }
            if (policyId != null) {
                predicates.add(cb.equal(root.get("policy").get("id"), policyId));
            }
            if (severity != null && !severity.isBlank()) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            if (actionTaken != null && !actionTaken.isBlank()) {
                predicates.add(cb.equal(root.get("actionTaken"), actionTaken));
            }

            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
