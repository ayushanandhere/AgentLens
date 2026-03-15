package dev.ayush.agentlens.kafka;

import dev.ayush.agentlens.audit.AuditLog;
import dev.ayush.agentlens.audit.AuditLogRepository;
import dev.ayush.agentlens.kafka.event.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

    private final AuditLogRepository auditLogRepository;

    @KafkaListener(topics = "audit-events", groupId = "agentlens-audit")
    public void consume(AuditEvent event) {
        log.info("Consumed audit event: [{}] {} on {} {}",
                event.getActor(), event.getAction(), event.getResourceType(), event.getResourceId());

        AuditLog auditLog = AuditLog.builder()
                .actor(event.getActor())
                .action(event.getAction())
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .details(event.getDetails())
                .timestamp(event.getTimestamp() != null
                        ? LocalDateTime.ofInstant(event.getTimestamp(), ZoneId.systemDefault())
                        : LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Saved audit log entry for [{}/{}]", event.getAction(), event.getResourceId());
    }
}
