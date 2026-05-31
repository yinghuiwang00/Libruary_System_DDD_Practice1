package com.library.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for all end-to-end cross-context integration tests.
 * Loads the combined Spring context with all 7 bounded contexts
 * and configures an embedded Kafka broker with all 5 event topics.
 */
@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
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
@DirtiesContext
public abstract class BaseEndToEndTest {

    protected static final String CATALOG_TOPIC = "library.catalog.events";
    protected static final String CIRCULATION_TOPIC = "library.circulation.events";
    protected static final String PATRON_TOPIC = "library.patron.events";
    protected static final String INVENTORY_TOPIC = "library.inventory.events";
    protected static final String PAYMENT_TOPIC = "library.payment.events";

    @Autowired
    protected EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    protected KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    protected ObjectMapper objectMapper;

    protected KafkaTemplate<String, String> kafkaTemplate;

    protected void setUpKafka() {
        // Wait for all Kafka listener containers to be assigned
        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());
        }

        // Create producer for sending raw JSON strings
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(pf);
    }

    protected void cleanAllTables() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.executeWithoutResult(status -> {
            // Clean in reverse dependency order to respect FK constraints
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
    }

    /**
     * Build a JSON event string with the required eventType field.
     */
    protected String buildEventJson(String eventType, String... keyValuePairs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"eventType\":\"").append(eventType).append("\"");
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            sb.append(",\"").append(keyValuePairs[i]).append("\":").append(keyValuePairs[i + 1]);
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Build a JSON object string for nested ID fields.
     */
    protected String idObject(String idValue) {
        return "{\"value\":\"" + idValue + "\"}";
    }

    /**
     * Build a JSON string value.
     */
    protected String jsonString(String value) {
        return "\"" + value + "\"";
    }

    /**
     * Send an event to a Kafka topic and return the JSON node for verification.
     */
    protected void sendEvent(String topic, String eventJson) {
        kafkaTemplate.send(topic, eventJson);
    }

    /**
     * Parse a JSON string to JsonNode for field extraction.
     */
    protected JsonNode parseJson(String json) throws Exception {
        return objectMapper.readTree(json);
    }
}
