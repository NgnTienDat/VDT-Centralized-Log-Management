Với kiến trúc Distributed Event-Driven này, đây là cấu trúc tôi đề xuất theo hướng **monorepo** — phù hợp nhất cho dự án học thuật/team nhỏ vì dễ quản lý, dễ chạy toàn bộ bằng một lệnh docker-compose.

---

## Cấu trúc tổng quan

```
/Log-Monitor-And-Alert-System/
│
├── pom.xml                          ← Maven parent (quản lý tất cả Java modules)
├── README.md
│
├── docs/                            ← Tài liệu kỹ thuật
├── services/                        ← Toàn bộ backend Spring Boot
├── frontend/                        ← React/Vue dashboard
├── infrastructure/                  ← Kafka, ES, Logstash, Redis, Kibana config
└── scripts/                         ← Load test, init tools
```

---

## Chi tiết từng phần

### `docs/`
```
docs/
├── architecture.md                  ← Mô tả kiến trúc tổng quan + diagram
├── adr/                             ← Lý do chọn công nghệ (ghi lại quyết định)
│   ├── 001-kafka-over-rabbitmq.md
│   ├── 002-logstash-as-processor.md
│   └── 003-elasticsearch-storage.md
├── api-spec/
│   ├── ingestion-api.yaml           ← OpenAPI spec: POST /api/v1/logs
│   └── query-api.yaml               ← OpenAPI spec: GET /api/v1/logs, metrics
└── database-schema.md               ← ES index mapping + Redis key design
```

> **Tại sao có `adr/`?** Sau 2 tuần build, bạn sẽ quên lý do mình chọn Kafka thay vì RabbitMQ. ADR (Architecture Decision Records) ghi lại context + lý do để team không tranh luận lại từ đầu.

---

### `services/` — Maven multi-module
```
services/
│
├── shared-lib/                      ← Thư viện dùng chung, KHÔNG deploy riêng
│   ├── src/main/java/com/logmonitor/shared/
│   │   ├── dto/
│   │   │   ├── LogEntryDTO.java      ← Cấu trúc log chuẩn hoá
│   │   │   └── AlertEventDTO.java
│   │   ├── enums/
│   │   │   ├── LogLevel.java         ← INFO, WARN, ERROR, CRITICAL
│   │   │   └── LogStatus.java        ← RAW, NORMALIZED, STORED
│   │   └── constants/
│   │       └── KafkaTopics.java      ← "raw-logs", "critical-alerts"
│   └── pom.xml
│
├── ingestion-api/                   ← Service 1: Nhận log từ producers
│   ├── src/main/java/com/logmonitor/ingestion/
│   │   ├── IngestionApiApplication.java
│   │   ├── controller/
│   │   │   └── LogIngestionController.java
│   │   ├── service/
│   │   │   └── LogIngestionService.java
│   │   ├── kafka/
│   │   │   └── LogKafkaProducer.java
│   │   ├── validation/
│   │   │   └── LogEntryValidator.java
│   │   ├── ratelimit/
│   │   │   └── RateLimitFilter.java
│   │   └── config/
│   │       ├── KafkaConfig.java
│   │       └── SecurityConfig.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── application-prod.yml
│   ├── src/test/
│   ├── Dockerfile
│   └── pom.xml
│
├── query-api/                       ← Service 2: Search, filter, live stream
│   ├── src/main/java/com/logmonitor/query/
│   │   ├── QueryApiApplication.java
│   │   ├── controller/
│   │   │   ├── LogQueryController.java   ← GET /logs, /metrics, /analytics
│   │   │   └── LiveStreamController.java ← WebSocket endpoint cho live log
│   │   ├── service/
│   │   │   ├── LogQueryService.java
│   │   │   └── LiveStreamService.java
│   │   ├── elasticsearch/
│   │   │   └── ElasticsearchRepository.java
│   │   ├── websocket/
│   │   │   └── LiveStreamWebSocketHandler.java
│   │   └── config/
│   │       ├── ElasticsearchConfig.java
│   │       └── WebSocketConfig.java
│   ├── src/main/resources/
│   ├── src/test/
│   ├── Dockerfile
│   └── pom.xml
│
└── alert-service/                   ← Service 3: Phát hiện lỗi + notify
    ├── src/main/java/com/logmonitor/alert/
    │   ├── AlertServiceApplication.java
    │   ├── kafka/
    │   │   └── CriticalAlertConsumer.java  ← Consume topic critical-alerts
    │   ├── service/
    │   │   ├── AlertDetectionService.java
    │   │   └── DeduplicationService.java   ← Redis TTL lock logic
    │   ├── notifier/
    │   │   ├── TelegramNotifier.java
    │   │   ├── EmailNotifier.java
    │   │   └── WebSocketAlertPusher.java
    │   ├── redis/
    │   │   └── RedisLockRepository.java
    │   └── config/
    │       ├── KafkaConfig.java
    │       ├── RedisConfig.java
    │       └── WebSocketConfig.java
    ├── src/main/resources/
    ├── src/test/
    ├── Dockerfile
    └── pom.xml
```

> **Tại sao có `shared-lib`?** `LogEntryDTO`, `KafkaTopics` constants nếu viết lại trong từng service sẽ dẫn đến **typo bug kinh điển** — service A push topic `"raw-logs"`, service B consume `"raw_logs"` → không kết nối được. Dùng shared-lib compile-time thì lỗi này bị bắt ngay.

---

### `frontend/`
```
frontend/
└── dashboard/                       ← React hoặc Vue
    ├── src/
    │   ├── components/
    │   │   ├── LogTable/             ← Bảng hiển thị log, lọc level/app
    │   │   ├── LiveStream/           ← WebSocket consumer, real-time feed
    │   │   ├── AlertBanner/          ← Toast/popup khi có ERROR
    │   │   └── Charts/              ← Error rate theo giờ, theo app
    │   ├── services/
    │   │   ├── queryApi.js           ← Gọi query-api REST
    │   │   └── websocket.js          ← Kết nối WebSocket
    │   ├── pages/
    │   │   ├── Dashboard.jsx
    │   │   ├── LiveMonitor.jsx
    │   │   └── Analytics.jsx
    │   └── main.jsx
    ├── Dockerfile
    ├── nginx.conf                    ← Serve static + proxy /api → backend
    └── package.json
```

---

### `infrastructure/`
```
infrastructure/
│
├── docker-compose.yml               ← Full stack (tất cả services + infra)
├── docker-compose.infra.yml         ← Chỉ Kafka, ES, Redis, Kibana (dev mode)
├── .env.example                     ← Template biến môi trường
│
├── kafka/
│   └── init-topics.sh               ← Tạo topic raw-logs + critical-alerts
│
├── elasticsearch/
│   ├── elasticsearch.yml
│   ├── index-templates/
│   │   └── logs-template.json       ← Mapping: app_name, level, message, trace_id
│   └── ilm-policies/
│       └── logs-retention-policy.json  ← Hot 1d → Warm 7d → Delete INFO
│
├── logstash/
│   ├── logstash.yml
│   └── pipeline/
│       ├── 01-input.conf             ← Input: Kafka topic raw-logs
│       ├── 02-filter.conf            ← Grok parse + normalize + detect level
│       └── 03-output.conf            ← Output: ES + Kafka critical-alerts
│
├── redis/
│   └── redis.conf
│
└── kibana/
    └── kibana.yml
```

> **Tại sao tách `01-input`, `02-filter`, `03-output`?** Logstash pipeline dài trong 1 file rất khó debug. Tách ra thì khi filter bị lỗi, bạn biết ngay vấn đề ở `02-filter.conf` mà không phải đọc lại toàn bộ.

---

### `scripts/`
```
scripts/
├── load-test/
│   ├── fire-500-logs.sh             ← Bắn 500 logs trong 2 giây (demo)
│   └── generate-payload.py          ← Tạo random log payloads
│
└── init/
    ├── 01-create-kafka-topics.sh
    └── 02-create-es-index.sh
```

---

## Luồng khởi động cả hệ thống

```bash
# Bước 1: Khởi động toàn bộ infrastructure
docker-compose -f infrastructure/docker-compose.infra.yml up -d

# Bước 2: Init Kafka topics + ES index
./scripts/init/01-create-kafka-topics.sh
./scripts/init/02-create-es-index.sh

# Bước 3: Khởi động toàn bộ services
docker-compose -f infrastructure/docker-compose.yml up -d

# Bước 4: Chạy load test demo
./scripts/load-test/fire-500-logs.sh
```

```
📂 my-lab-project/
├── 📂 logs-app/          (Ứng dụng chạy chính, tự sinh log)
│   ├── 📂infras/
│   │   └──📂filebeat/
│   │       └── filebeat.yml            (Cấu hình Filebeat)
│   └── 📂 logs/
│   │   └── logs-app.log     (File log sẽ được sinh ra ở đây)
│   └── docker-compose.yml
│
└── 📂 log-monitor-backend/              (Hệ thống giám sát log của bạn)
    ├── docker-compose.yml   (Quản lý Logstash container)
    📂 logstash/
        └── logstash.conf        (Cấu hình Logstash)

```



---

## Tóm tắt quyết định kiến trúc

| Quyết định | Lý do |
|---|---|
| **Monorepo** | Dễ chạy toàn bộ, 1 git repo, phù hợp team nhỏ |
| **Maven multi-module** | Shared-lib compile cùng lúc, tránh lỗi typo Kafka topic |
| **3 services riêng biệt** | Mỗi service scale độc lập, ingestion không ảnh hưởng alert |
| **Logstash pipeline tách 3 file** | Dễ debug, dễ thay filter rule mà không đụng input/output |
| **2 docker-compose file** | Dev chỉ chạy infra, CI/CD chạy full stack |