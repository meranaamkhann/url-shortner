package com.urlshortener.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.replication-factor:1}")
    private short replicationFactor;

    @Bean
    public NewTopic clickEventsTopic() {
        return TopicBuilder.name("click-events")
                .partitions(6)
                .replicas(replicationFactor)
                .config("retention.ms", String.valueOf(7L * 24 * 60 * 60 * 1000)) // 7 days
                .build();
    }

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name("audit-events")
                .partitions(3)
                .replicas(replicationFactor)
                .config("retention.ms", String.valueOf(90L * 24 * 60 * 60 * 1000)) // 90 days, compliance
                .build();
    }

    @Bean
    public NewTopic clickEventsDlqTopic() {
        return TopicBuilder.name("click-events-dlq")
                .partitions(3)
                .replicas(replicationFactor)
                .build();
    }
}
