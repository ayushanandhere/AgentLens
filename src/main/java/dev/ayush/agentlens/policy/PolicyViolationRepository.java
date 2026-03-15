package dev.ayush.agentlens.policy;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PolicyViolationRepository extends JpaRepository<PolicyViolation, UUID>, JpaSpecificationExecutor<PolicyViolation> {

    List<PolicyViolation> findByTraceId(UUID traceId);

    @Override
    @EntityGraph(attributePaths = {"trace", "policy"})
    java.util.Optional<PolicyViolation> findById(UUID id);
}
