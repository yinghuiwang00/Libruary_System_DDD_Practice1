# Troubleshooting Guide

> Common issues and solutions when building, testing, and running the Library Management System.

---

## Build Issues

### Maven Multi-Module Build Fails

**Symptom**: `mvn clean install` fails with "Cannot resolve dependencies"

**Cause**: Module build order or local cache corruption

**Solution**:
```bash
# Clean and rebuild from root
mvn clean install -DskipTests

# If still failing, clear local cache
rm -rf ~/.m2/repository/com/library/
mvn clean install -DskipTests
```

### Compilation Error: Cannot Find library-shared Classes

**Symptom**: `package com.library.shared does not exist`

**Cause**: `library-shared` not built yet

**Solution**:
```bash
# Build shared module first
cd library-shared && mvn clean install
cd .. && mvn clean install -DskipTests
```

### Lombok/MapStruct Processing Error

**Symptom**: `cannot find symbol` for generated methods

**Cause**: Annotation processors not configured

**Solution**: Verify `pom.xml` has:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

## Test Issues

### EmbeddedKafka Port Conflict

**Symptom**: `Port 9092 already in use` or `Failed to start Kafka`

**Cause**: A previous test run left EmbeddedKafka running, or real Kafka is running locally

**Solution**:
```bash
# Kill any process on port 9092
lsof -ti:9092 | xargs kill -9 2>/dev/null

# Or stop local Kafka
docker stop library-kafka 2>/dev/null

# Then re-run tests
mvn test
```

### H2 PostgreSQL Mode Compatibility

**Symptom**: Test passes with H2 but fails with PostgreSQL in staging

**Cause**: H2's PostgreSQL compatibility mode doesn't support all PostgreSQL features

**Common differences**:
- H2 auto-commits DDL; PostgreSQL requires explicit transactions
- H2 treats `VARCHAR` differently for length constraints
- H2 sequence generation may differ

**Solution**: Run staging tests to catch these:
```bash
mvn test -Pstaging -pl library-staging-test
```

### Cucumber Tests Not Found by Surefire

**Symptom**: `Tests run: 0` but `.feature` files exist

**Cause**: Surefire not configured to find Cucumber test suite

**Solution**: Verify `pom.xml` surefire includes:
```xml
<includes>
    <include>**/CucumberTestSuite.java</include>
</includes>
```

### Staging Test Database Connection Failed

**Symptom**: `Connection refused` or `FATAL: password authentication failed`

**Cause**: PostgreSQL not running or password mismatch

**Solution**:
```bash
# Verify PostgreSQL is running
docker ps | grep postgres

# Set correct password
export DB_PASSWORD=postgres

# Or start PostgreSQL
docker run -d --name library-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:16
```

---

## Kafka Issues

### Consumer Not Receiving Events

**Symptom**: Event published but handler never called

**Possible causes and solutions**:

1. **Kafka not running**:
   ```bash
   # For local dev
   docker start library-zookeeper library-kafka
   ```

2. **Wrong topic name**: Check `@KafkaListener(topics = "...")` matches publisher's topic

3. **Consumer group lag**: Consumer may be processing old events
   ```bash
   # Reset consumer group offset (staging test does this in @BeforeAll)
   kafka-consumer-groups --bootstrap-server localhost:29092 \
     --group <group-id> --reset-offsets --to-latest --execute --topic <topic>
   ```

4. **Deserialization error**: Check consumer logs for `Error processing ... event`

### Kafka Container Fails to Start in CI

**Symptom**: CI staging job fails with Kafka connection errors

**Cause**: Port mismatch between `KAFKA_LISTENERS` and `KAFKA_ADVERTISED_LISTENERS`

**Solution**: Ensure both use port 29092:
```yaml
KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:29092
```

### Poison Pill Message

**Symptom**: Consumer stuck, not processing new messages

**Cause**: A message that causes the consumer to throw an unhandled exception repeatedly

**Solution**: All consumers should catch exceptions:
```java
catch (Exception e) {
    log.error("Error processing event: {}", e.getMessage(), e);
    // Do NOT re-throw — skip the poison pill
}
```

---

## General Tips

### Run Tests for Single Module

```bash
cd library-catalog && mvn test
```

### Run Single Test Class

```bash
mvn test -Dtest=LoanTest -pl library-circulation
```

### Run Single Test Method

```bash
mvn test -Dtest=LoanTest#testCheckout -pl library-circulation
```

### View Test Reports

After `mvn test`, reports are in:
```
library-<context>/target/surefire-reports/
```

### Debug Kafka Events

Use Kafka UI at http://localhost:9000 (if running) to:
- Browse topics and messages
- Check consumer group offsets
- Monitor throughput
