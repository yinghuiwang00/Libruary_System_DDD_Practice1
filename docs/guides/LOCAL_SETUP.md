# Guide: Local Development Setup

> How to start the Library Management System locally for development and testing.

---

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| JDK | 17 | Runtime |
| Maven | 3.8+ | Build |
| Docker | 20+ | Infrastructure services |
| Git | 2.x | Version control |

---

## Step 1: Start Infrastructure Services

### PostgreSQL

```bash
docker run -d \
  --name library-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16
```

### Kafka + Zookeeper

```bash
# Zookeeper
docker run -d \
  --name library-zookeeper \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -p 2181:2181 \
  confluentinc/cp-zookeeper:7.5.0

# Kafka
docker run -d \
  --name library-kafka \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=host.docker.internal:2181 \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:29092 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:29092 \
  -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
  -p 29092:29092 \
  --link library-zookeeper \
  confluentinc/cp-kafka:7.5.0
```

### Redis (optional)

```bash
docker run -d --name library-redis -p 6379:6379 redis:7
```

---

## Step 2: Create Databases

```bash
docker exec -it library-postgres psql -U postgres -c "
  CREATE DATABASE library_catalog;
  CREATE DATABASE library_inventory;
  CREATE DATABASE library_circulation;
  CREATE DATABASE library_patron;
  CREATE DATABASE library_payment;
  CREATE DATABASE library_analytics;
  CREATE DATABASE library_notification;
"
```

> Hibernate `ddl-auto: update` will create tables automatically on first startup.

---

## Step 3: Build All Modules

```bash
cd /path/to/Libruary_System_DDD_Practice1
mvn clean install -DskipTests
```

---

## Step 4: Start Modules

Start each bounded context in a separate terminal:

```bash
# Terminal 1: Catalog (port 8081)
cd library-catalog && mvn spring-boot:run

# Terminal 2: Inventory (port 8082)
cd library-inventory && mvn spring-boot:run

# Terminal 3: Circulation (port 8083)
cd library-circulation && mvn spring-boot:run

# Terminal 4: Patron (port 8084)
cd library-patron && mvn spring-boot:run

# Terminal 5: Payment (port 8085)
cd library-payment && mvn spring-boot:run

# Terminal 6: Analytics (port 8086)
cd library-analytics && mvn spring-boot:run

# Terminal 7: Notification (port 8087)
cd library-notification && mvn spring-boot:run
```

### Environment Variables

If your PostgreSQL password differs from the default, set before starting:

```bash
export DB_PASSWORD=your_password
```

---

## Step 5: Verify

### Swagger UI (API Docs)

| Module | URL |
|--------|-----|
| Catalog | http://localhost:8081/swagger-ui.html |
| Inventory | http://localhost:8082/swagger-ui.html |
| Circulation | http://localhost:8083/swagger-ui.html |
| Patron | http://localhost:8084/swagger-ui.html |
| Payment | http://localhost:8085/swagger-ui.html |
| Analytics | http://localhost:8086/swagger-ui.html |
| Notification | http://localhost:8087/swagger-ui.html |

### Quick Smoke Test

```bash
# Create a book in Catalog
curl -X POST http://localhost:8081/api/catalog/books \
  -H "Content-Type: application/json" \
  -d '{"title":"Test Book","isbn":"978-0-13-468599-1"}'

# Register a patron
curl -X POST http://localhost:8084/api/patrons \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","type":"STUDENT"}'
```

---

## Step 6: Monitoring (Optional)

| Tool | URL | Credentials |
|------|-----|-------------|
| Kafka UI | http://localhost:9000 | — |
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | admin/admin |
| Jaeger | http://localhost:16686 | — |

> These require additional Docker containers — see `docker-compose.yml` if available.

---

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| `Connection refused` on port 5432 | PostgreSQL not running | `docker start library-postgres` |
| Kafka consumer not receiving events | Kafka not running | `docker start library-zookeeper library-kafka` |
| `Table not found` errors | Hibernate ddl-auto wrong | Set `ddl-auto: update` in `application.yml` |
| Port already in use | Module already running | `lsof -i :8081` to find and kill |
