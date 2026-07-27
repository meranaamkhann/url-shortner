package com.urlshortener.kafka;

import com.urlshortener.dto.event.AuditEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEventProducer {

    private static final String TOPIC = "audit-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(AuditEventMessage message) {
        String key = message.actorId() != null ? message.actorId() : "anonymous";
        kafkaTemplate.send(TOPIC, key, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish audit event action={} entityId={}",
                                message.action(), message.entityId(), ex);
                    }
                });
    }
}
