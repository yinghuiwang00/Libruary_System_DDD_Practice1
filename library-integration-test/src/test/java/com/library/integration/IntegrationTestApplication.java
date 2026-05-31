package com.library.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Combined Spring Boot application for end-to-end cross-context integration tests.
 *
 * Instead of scanning broad module packages (which causes bean name conflicts
 * across modules for identically-named classes like JpaConfig, GlobalExceptionHandler,
 * CirculationEventConsumer, etc.), we scan only the specific sub-packages needed:
 * - domain.model (entities)
 * - domain.service (domain services)
 * - domain.repository (JPA repositories)
 * - domain.event (domain events)
 * - domain.exception (domain exceptions)
 * - application.handler (event handlers for cross-context integration)
 * - application.service (application services)
 * - application.command, application.query, application.dto
 * - infrastructure.messaging (Kafka publishers and consumers)
 * - shared (shared domain concepts)
 *
 * Excluded on purpose:
 * - interfaces.rest (controllers, GlobalExceptionHandler — not needed in E2E tests)
 * - infrastructure.config (JpaConfig per module — we declare @EntityScan/@EnableJpaRepositories centrally)
 * - infrastructure.persistence (custom repository impls — not needed for basic E2E tests)
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    // Integration test config (provides beans like CirculationPolicy)
    "com.library.integration.config",
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
public class IntegrationTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntegrationTestApplication.class, args);
    }
}
