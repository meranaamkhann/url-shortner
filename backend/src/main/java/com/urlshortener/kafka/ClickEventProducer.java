package com.urlshortener.kafka;

import com.urlshortener.dto.event.ClickEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Publishes a ClickEventMessage for every redirect. Partition key is the urlId so all
 * events for a given short URL land on the same partition (ordered processing, and a
 * natural unit for any future per-URL stateful aggregation in the consumer).
 *
 * This call is fire-and-forget from the redirect controller's perspective — we do not
 * block the user's redirect response on Kafka ack. A failed publish here means a click
 * undercount in analytics, which is an acceptable trade-off against adding Kafka latency
 * (or a Kafka outage) to the critical user-facing redirect path.
 *
 * @Async matters here beyond the ack: KafkaTemplate.send() itself (not just the returned
 * future) can block the calling thread for up to producer max.block.ms while metadata for
 * the topic is unavailable — e.g. if Kafka isn't reachable at all, which is a real
 * deployment shape (see application.yml comment on max.block.ms). Running this on Spring's
 * async executor (@EnableAsync is on in UrlShortenerApplication) means that wait, however
 * long it turns out to be, never touches the redirect request thread.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClickEventProducer {

    private static final String TOPIC = "click-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publish(ClickEventMessage message) {
        kafkaTemplate.send(TOPIC, message.urlId(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish click event for urlId={}", message.urlId(), ex);
                    } else {
                        log.debug("Published click event for urlId={} to partition={}",
                                message.urlId(), result.getRecordMetadata().partition());
                    }
                });
    }
}
