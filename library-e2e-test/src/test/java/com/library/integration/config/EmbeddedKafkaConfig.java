package com.library.integration.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.context.EmbeddedKafka;

/**
 * Shared Embedded Kafka configuration for all E2E test classes.
 * All 5 per-context topics are configured here.
 */
@Configuration
@EmbeddedKafka(
    partitions = 1,
    topics = {
        "library.catalog.events",
        "library.circulation.events",
        "library.patron.events",
        "library.inventory.events",
        "library.payment.events"
    }
)
public class EmbeddedKafkaConfig {
}
