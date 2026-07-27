package com.urlshortener.service;

import com.urlshortener.domain.enums.AuditAction;
import com.urlshortener.dto.event.AuditEventMessage;
import com.urlshortener.kafka.AuditEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditEventProducer auditEventProducer;

    public void log(UUID actorId, AuditAction action, String entityType, String entityId,
                     String ipAddress, Map<String, Object> metadata) {
        try {
            AuditEventMessage message = new AuditEventMessage(
                    actorId != null ? actorId.toString() : null,
                    action.name(),
                    entityType,
                    entityId,
                    ipAddress,
                    metadata,
                    Instant.now()
            );
            auditEventProducer.publish(message);
        } catch (Exception e) {
            // Audit logging is best-effort by design: it must never roll back or block
            // the business transaction that triggered it.
            log.error("Failed to publish audit event for action={} entityType={} entityId={}",
                    action, entityType, entityId, e);
        }
    }
}
