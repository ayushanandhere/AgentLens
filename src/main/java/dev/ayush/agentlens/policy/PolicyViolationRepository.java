package dev.ayush.agentlens.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PolicyViolationRepository extends JpaRepository<PolicyViolation, UUID> {

    List<PolicyViolation> findByTraceId(UUID traceId);
}
