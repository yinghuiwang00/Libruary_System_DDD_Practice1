package com.library.integration.bdd;

import com.library.integration.IntegrationTestApplication;
import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Cucumber-Spring glue for BDD integration tests.
 * Combines CucumberContextConfiguration with the setup logic from BaseEndToEndTest.
 */
@SpringBootTest(
    classes = IntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DirtiesContext
@Import(E2EScenarioState.class)
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
@CucumberContextConfiguration
public class CucumberSpringConfig {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private KafkaTemplate<String, String> kafkaTemplate;

    @Before
    public void setUp() {
        // Wait for all Kafka listener containers to be assigned
        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());
        }

        // Create producer
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(pf);

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
    }

    public KafkaTemplate<String, String> getKafkaTemplate() {
        return kafkaTemplate;
    }
}
