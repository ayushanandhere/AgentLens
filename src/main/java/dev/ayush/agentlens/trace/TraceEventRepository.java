package dev.ayush.agentlens.trace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TraceEventRepository extends JpaRepository<TraceEvent, UUID> {

    List<TraceEvent> findByTraceIdOrderBySequenceNumAsc(UUID traceId);

    long countByTraceId(UUID traceId);
}
