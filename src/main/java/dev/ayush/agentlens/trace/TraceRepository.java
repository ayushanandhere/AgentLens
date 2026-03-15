package dev.ayush.agentlens.trace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TraceRepository extends JpaRepository<Trace, UUID>, JpaSpecificationExecutor<Trace> {

    List<Trace> findByAgentId(UUID agentId);

    List<Trace> findByStatus(String status);

    List<Trace> findByTenantId(String tenantId);

    long countByAgentId(UUID agentId);
}
