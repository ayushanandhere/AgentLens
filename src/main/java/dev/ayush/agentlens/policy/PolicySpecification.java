package dev.ayush.agentlens.policy;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class PolicySpecification {

    private PolicySpecification() {
    }

    public static Specification<Policy> withFilters(String type, String scope, Boolean enabled) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("policyType"), type));
            }
            if (scope != null && !scope.isBlank()) {
                predicates.add(cb.equal(root.get("scope"), scope));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }

            query.orderBy(cb.desc(root.get("updatedAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
