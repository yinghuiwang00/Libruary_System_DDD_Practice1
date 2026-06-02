package com.library.staging.bdd;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Inspects and displays real-time state of staging test infrastructure.
 * Shows PostgreSQL table row counts and Kafka topic offsets.
 * Output goes to both SLF4J log and stdout (visible in Maven test output).
 */
@Component
public class StagingEnvironmentInspector {

    private static final Logger log = LoggerFactory.getLogger(StagingEnvironmentInspector.class);

    private static final List<String> TABLES = List.of(
        "books", "book_authors", "book_categories", "book_copies",
        "copy_inventories", "libraries", "patrons", "loans",
        "payments", "analytics_reports", "notifications"
    );

    private static final List<String> TOPICS = List.of(
        "library.catalog.events",
        "library.circulation.events",
        "library.patron.events",
        "library.inventory.events",
        "library.payment.events"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${spring.kafka.bootstrap-servers:localhost:29092}")
    private String bootstrapServers;

    /**
     * Print a full environment snapshot: PostgreSQL + Kafka + Redis.
     */
    public void printSnapshot(String phase) {
        String banner = "═══════════════════════════════════════════════════════";
        String header = String.format("  STAGING ENV SNAPSHOT  ──  %s", phase);

        System.out.println("\n" + banner);
        System.out.println(header);
        System.out.println(banner);

        printPostgreSQL();
        printKafka();
        printRedis();

        System.out.println(banner + "\n");
    }

    /**
     * Print PostgreSQL table row counts.
     */
    private void printPostgreSQL() {
        System.out.println("  ┌─ PostgreSQL ─────────────────────────────────────");
        System.out.println("  │ Table               │ Rows");
        System.out.println("  │─────────────────────┼──────────");

        Map<String, Integer> counts = new TreeMap<>();
        for (String table : TABLES) {
            try {
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table, Integer.class);
                counts.put(table, count != null ? count : 0);
            } catch (Exception e) {
                counts.put(table, -1);  // table doesn't exist yet
            }
        }

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String rows = entry.getValue() < 0 ? "N/A" : String.valueOf(entry.getValue());
            System.out.printf("  │ %-19s │ %s%n", entry.getKey(), rows);
        }
        System.out.println("  └─────────────────────────────────────────────────");
    }

    /**
     * Print Kafka topic end offsets (total messages per topic).
     */
    private void printKafka() {
        System.out.println("  ┌─ Kafka ──────────────────────────────────────────");
        System.out.println("  │ Topic                          │ Messages");
        System.out.println("  │────────────────────────────────┼──────────");

        try (AdminClient admin = AdminClient.create(
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {

            Map<String, Long> topicOffsets = new TreeMap<>();
            for (String topic : TOPICS) {
                try {
                    TopicPartition tp = new TopicPartition(topic, 0);
                    ListOffsetsResult offsetsResult = admin.listOffsets(
                        Map.of(tp, OffsetSpec.latest()));
                    Long offset = offsetsResult.partitionResult(tp)
                        .get(5, TimeUnit.SECONDS).offset();
                    topicOffsets.put(topic, offset != null ? offset : 0);
                } catch (Exception e) {
                    topicOffsets.put(topic, -1L);  // topic doesn't exist
                }
            }

            for (Map.Entry<String, Long> entry : topicOffsets.entrySet()) {
                String msgs = entry.getValue() < 0 ? "N/A" : String.valueOf(entry.getValue());
                System.out.printf("  │ %-30s │ %s%n", entry.getKey(), msgs);
            }

            // Show consumer group lag if exists
            try {
                var groups = admin.listConsumerGroups().all().get(5, TimeUnit.SECONDS);
                var stagingGroup = groups.stream()
                    .filter(g -> g.groupId().contains("staging"))
                    .findFirst();
                if (stagingGroup.isPresent()) {
                    System.out.printf("  │ Consumer Group: %-30s │ active%n",
                        stagingGroup.get().groupId());
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            System.out.printf("  │ Error: %-41s │%n", e.getMessage());
        }

        System.out.println("  └─────────────────────────────────────────────────");
    }

    /**
     * Print Redis status. Currently not used in the project,
     * but included as a placeholder for future use.
     */
    private void printRedis() {
        System.out.println("  ┌─ Redis ──────────────────────────────────────────");
        System.out.println("  │ Status: Not used in current tests");
        System.out.println("  │ (Docker running at localhost:6379, no app dependency)");
        System.out.println("  └─────────────────────────────────────────────────");
    }
}
