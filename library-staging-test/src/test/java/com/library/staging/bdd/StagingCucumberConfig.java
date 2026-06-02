package com.library.staging.bdd;

import com.library.staging.StagingTestApplication;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.spring.CucumberContextConfiguration;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Cucumber-Spring glue for staging E2E tests against real Docker infrastructure.
 *
 * Lifecycle:
 *   @Before (first scenario) → delete+recreate Kafka topics, wait for consumers
 *   @Before (every scenario) → clean PostgreSQL tables
 *   @AfterAll                → stop consumers, delete Kafka topics & consumer groups
 *
 * The @AfterAll runs after ALL scenarios complete but BEFORE the Spring context
 * is destroyed, making it more reliable than @PreDestroy for cleanup.
 */
@SpringBootTest(
    classes = StagingTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import({StagingScenarioState.class, StagingEnvironmentInspector.class})
@CucumberContextConfiguration
public class StagingCucumberConfig {

    private static final Logger log = LoggerFactory.getLogger(StagingCucumberConfig.class);

    private static final List<String> TOPICS = List.of(
        "library.catalog.events",
        "library.circulation.events",
        "library.patron.events",
        "library.inventory.events",
        "library.payment.events"
    );

    private static final Set<String> CONSUMER_GROUPS = Set.of(
        "library.analytics.consumer.catalog",
        "library.analytics.consumer.inventory",
        "library.notification.consumer.patron",
        "library.notification.consumer.circulation",
        "library.notification.consumer.inventory",
        "library.notification.consumer.payment",
        "library.circulation.consumer.patron",
        "library.patron.consumer.circulation",
        "library.patron.consumer.payment",
        "library.payment.consumer.circulation",
        "library.inventory.consumer.catalog",
        "library.inventory.consumer.circulation"
    );

    /** Static reference to Spring ApplicationContext for @AfterAll (which is static, no injection). */
    private static volatile ApplicationContext springContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private StagingEnvironmentInspector inspector;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private ApplicationContext applicationContext;

    private static boolean initialized = false;

    @Before
    public void setUp(Scenario scenario) {
        springContext = this.applicationContext;

        // Clean all DB tables (reverse dependency order for FK constraints)
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.executeWithoutResult(status -> {
            jdbcTemplate.execute("DELETE FROM notifications");
            jdbcTemplate.execute("DELETE FROM analytics_reports");
            jdbcTemplate.execute("DELETE FROM payments");
            jdbcTemplate.execute("DELETE FROM book_copies");
            jdbcTemplate.execute("DELETE FROM copy_inventories");
            jdbcTemplate.execute("DELETE FROM book_authors");
            jdbcTemplate.execute("DELETE FROM book_categories");
            jdbcTemplate.execute("DELETE FROM books");
            jdbcTemplate.execute("DELETE FROM patrons");
            jdbcTemplate.execute("DELETE FROM libraries");
        });

        // First scenario: clean Kafka and wait for consumers to be fully ready
        if (!initialized) {
            recreateTopicsAndDeleteGroups("before test run");
            waitForConsumerPartitionAssignment();
            initialized = true;
        }

        inspector.printSnapshot("BEFORE: " + scenario.getName());
    }

    @After
    public void tearDown(Scenario scenario) {
        inspector.printSnapshot("AFTER:  " + scenario.getName()
            + " [" + (scenario.isFailed() ? "FAILED" : "PASSED") + "]");
    }

    /**
     * Runs after ALL Cucumber scenarios complete, before Spring context destruction.
     * Only deletes topics and consumer groups — next test run's @Before will recreate.
     */
    @AfterAll
    public static void afterAll() {
        log.info("=== @AfterAll: Final Kafka cleanup ===");
        stopAllConsumers();
        deleteTopicsAndGroups();
        initialized = false;
    }

    // ──────────────────────────────────────────────────
    //  Kafka cleanup operations
    // ──────────────────────────────────────────────────

    /**
     * Delete topics, recreate them, then delete consumer groups.
     * Used by @Before to establish a clean Kafka state.
     */
    private static void recreateTopicsAndDeleteGroups(String phase) {
        String servers = resolveBootstrapServers();
        log.info("Cleaning Kafka topics and consumer groups ({})...", phase);

        try (AdminClient admin = AdminClient.create(
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, servers))) {

            // 1. Delete all test topics (clears messages and offsets)
            try {
                admin.deleteTopics(TOPICS).all().get(10, TimeUnit.SECONDS);
                log.info("[{}] Deleted {} topics", phase, TOPICS.size());
            } catch (Exception e) {
                log.warn("[{}] Topic deletion failed (may not exist): {}", phase, e.getMessage());
            }

            // 2. Recreate topics with original configuration (3 partitions, replication 1)
            List<NewTopic> newTopics = TOPICS.stream()
                .map(t -> new NewTopic(t, 3, (short) 1))
                .collect(Collectors.toList());
            admin.createTopics(newTopics).all().get(10, TimeUnit.SECONDS);
            log.info("[{}] Recreated {} topics (3 partitions each)", phase, newTopics.size());

            // 3. Delete consumer groups
            deleteConsumerGroups(admin, phase);

            log.info("[{}] Kafka cleanup complete.", phase);

        } catch (Exception e) {
            log.warn("[{}] Kafka cleanup failed (non-fatal): {}", phase, e.getMessage());
        }
    }

    /**
     * Delete topics and consumer groups without recreating.
     * Used by @AfterAll — next test run's @Before will recreate topics.
     */
    private static void deleteTopicsAndGroups() {
        String servers = resolveBootstrapServers();
        try (AdminClient admin = AdminClient.create(
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, servers))) {

            try {
                admin.deleteTopics(TOPICS).all().get(10, TimeUnit.SECONDS);
                log.info("[after test run] Deleted {} topics", TOPICS.size());
            } catch (Exception e) {
                log.warn("[after test run] Topic deletion failed: {}", e.getMessage());
            }

            deleteConsumerGroups(admin, "after test run");

        } catch (Exception e) {
            log.warn("[after test run] Cleanup failed: {}", e.getMessage());
        }
    }

    /** Delete all known library consumer groups via the given AdminClient. */
    private static void deleteConsumerGroups(AdminClient admin, String phase) {
        int deleted = 0;
        try {
            for (var group : admin.listConsumerGroups().all().get(10, TimeUnit.SECONDS)) {
                if (CONSUMER_GROUPS.contains(group.groupId())) {
                    try {
                        admin.deleteConsumerGroups(List.of(group.groupId()))
                            .all().get(5, TimeUnit.SECONDS);
                        deleted++;
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        log.info("[{}] Deleted {} consumer groups", phase, deleted);
    }

    /** Stop all Kafka listener containers so their consumer groups become inactive. */
    private static void stopAllConsumers() {
        if (springContext == null) return;
        try {
            var registry = springContext.getBean(KafkaListenerEndpointRegistry.class);
            for (var container : registry.getListenerContainers()) {
                try {
                    container.stop();
                } catch (Exception ignored) {}
            }
            log.info("[after test run] Stopped {} Kafka consumer containers",
                registry.getListenerContainers().size());
        } catch (Exception e) {
            log.warn("[after test run] Failed to stop consumers: {}", e.getMessage());
        }
    }

    private static String resolveBootstrapServers() {
        if (springContext != null) {
            try {
                return springContext.getEnvironment()
                    .getProperty("spring.kafka.bootstrap-servers", "localhost:29092");
            } catch (Exception ignored) {}
        }
        return "localhost:29092";
    }

    // ──────────────────────────────────────────────────
    //  Kafka consumer readiness check
    // ──────────────────────────────────────────────────

    private void waitForConsumerPartitionAssignment() {
        log.info("Waiting for Kafka consumer partition assignment...");
        var containers = kafkaListenerEndpointRegistry.getListenerContainers();

        for (var container : containers) {
            int maxAttempts = 60; // 30 seconds max per container
            for (int i = 0; i < maxAttempts; i++) {
                var assigned = container.getAssignedPartitions();
                if (assigned != null && !assigned.isEmpty()) {
                    log.debug("Container {} assigned {} partitions",
                        container.getGroupId(), assigned.size());
                    break;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long assignedCount = containers.stream()
            .filter(c -> c.getAssignedPartitions() != null && !c.getAssignedPartitions().isEmpty())
            .count();
        log.info("Kafka consumers ready: {}/{} containers have partition assignments",
            assignedCount, containers.size());
    }

    public KafkaTemplate<String, String> getKafkaTemplate() {
        return kafkaTemplate;
    }
}
