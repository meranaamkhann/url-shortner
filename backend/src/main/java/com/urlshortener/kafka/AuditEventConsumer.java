package com.urlshortener.kafka;

import com.urlshortener.domain.entity.AuditLog;
import com.urlshortener.domain.enums.AuditAction;
import com.urlshortener.dto.event.AuditEventMessage;
import com.urlshortener.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditLogRepository auditLogRepository;

    @KafkaListener(topics = "audit-events", groupId = "url-shortener-security-consumer")
    @Transactional
    public void onAuditEvent(AuditEventMessage message) {
        try {
            AuditLog entry = AuditLog.builder()
                    .actorId(message.actorId() != null ? UUID.fromString(message.actorId()) : null)
                    .action(AuditAction.valueOf(message.action()))
                    .entityType(message.entityType())
                    .entityId(message.entityId())
                    .ipAddress(message.ipAddress())
                    .metadata(message.metadata())
                    .createdAt(message.occurredAt())
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to persist audit event action={} entityType={} entityId={}: {}",
                    message.action(), message.entityType(), message.entityId(), message, e);
            throw e;
        }
    }
}
