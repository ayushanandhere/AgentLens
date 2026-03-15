package dev.ayush.agentlens.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID>, JpaSpecificationExecutor<Policy> {

    @Query("SELECT p FROM Policy p WHERE p.enabled = true AND (" +
            "p.scope = 'GLOBAL' OR " +
            "(p.scope = 'AGENT' AND p.scopeId = :agentId) OR " +
            "(p.scope = 'TENANT' AND p.scopeId = :tenantId))")
    List<Policy> findApplicablePolicies(@Param("agentId") String agentId,
                                        @Param("tenantId") String tenantId);
}
