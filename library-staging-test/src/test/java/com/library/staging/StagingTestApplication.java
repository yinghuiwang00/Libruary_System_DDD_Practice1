package com.library.staging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Combined Spring Boot application for staging E2E tests against real Docker infrastructure.
 *
 * Uses real PostgreSQL and Kafka instead of H2 + EmbeddedKafka.
 * Scans only specific sub-packages to avoid bean name conflicts across modules.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    // Staging test config (provides beans like CirculationPolicy)
    "com.library.staging.config",
    // Catalog
    "com.library.catalog.domain",
    "com.library.catalog.application",
    "com.library.catalog.infrastructure.messaging",
    "com.library.catalog.infrastructure.persistence",
    // Inventory
    "com.library.inventory.domain",
    "com.library.inventory.application",
    "com.library.inventory.infrastructure.messaging",
    // Circulation
    "com.library.circulation.domain",
    "com.library.circulation.application",
    "com.library.circulation.infrastructure.messaging",
    // Patron
    "com.library.patron.domain",
    "com.library.patron.application",
    "com.library.patron.infrastructure.messaging",
    // Payment
    "com.library.payment.domain",
    "com.library.payment.application",
    "com.library.payment.infrastructure.messaging",
    // Analytics
    "com.library.analytics.domain",
    "com.library.analytics.application",
    "com.library.analytics.infrastructure.messaging",
    // Notification
    "com.library.notification.domain",
    "com.library.notification.application",
    "com.library.notification.infrastructure.messaging",
    // Shared
    "com.library.shared"
})
@EntityScan(basePackages = {
    "com.library.catalog.domain.model",
    "com.library.inventory.domain.model",
    "com.library.circulation.domain.model",
    "com.library.patron.domain.model",
    "com.library.payment.domain.model",
    "com.library.analytics.domain.model",
    "com.library.notification.domain.model",
    "com.library.shared.domain.model"
})
@EnableJpaRepositories(basePackages = {
    "com.library.catalog.domain.repository",
    "com.library.inventory.domain.repository",
    "com.library.circulation.domain.repository",
    "com.library.patron.domain.repository",
    "com.library.payment.domain.repository",
    "com.library.analytics.domain.repository",
    "com.library.notification.domain.repository",
    "com.library.shared.domain.repository"
})
public class StagingTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(StagingTestApplication.class, args);
    }
}
