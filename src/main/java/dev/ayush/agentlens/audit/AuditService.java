package dev.ayush.agentlens.audit;

import dev.ayush.agentlens.audit.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listAuditLogs(String actor, String action,
                                                 String resourceType, String resourceId,
                                                 LocalDateTime startDate, LocalDateTime endDate,
                                                 Pageable pageable) {
        Specification<AuditLog> spec = AuditSpecification.withFilters(
                actor, action, resourceType, resourceId, startDate, endDate);
        return auditLogRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .timestamp(log.getTimestamp())
                .actor(log.getActor())
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .build();
    }
}
