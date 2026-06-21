# TASK: Implement Rule-Based Alerting Module for Log Monitor Backend

## 1. Project Context & Architectural Boundaries
- **Root Path:** `d:\viettel_digital_talent\VDT-Centralized-Log-Management\log-monitor-backend`
- **Current Architecture:** Filebeat -> Logstash -> Elasticsearch. Logstash also dual-writes via HTTP POST to the Spring Boot `Collector` module, which streams logs to the Frontend via WebSocket.
- **Strict Boundary Constraint:** DO NOT implement alert detection inside the existing `Collector` module or intercept the log stream. The new Alerting Module must be **completely independent and decoupled**. It will operate via a polling mechanism (Scheduled job) directly against Elasticsearch. 

## 2. Core Goal & Workflow
Implement a rule-based alerting engine that periodically queries Elasticsearch to detect threshold violations and broadcasts alerts to the frontend.

**Workflow:**
`Scheduler` -> `Load Rules (Memory)` -> `Query ES Count API` -> `Evaluate Threshold` -> `Check Cooldown` -> `Generate AlertEvent` -> `Publish via WebSocket`

### Sliding Window Semantics

The field `windowMinutes` represents a sliding time window.

Example:

- `windowMinutes = 5`
- Current time = 10:00

The query must evaluate logs with:

timestamp >= 09:55

The time window continuously moves forward on every scheduler execution.

DO NOT implement sliding window using in-memory queues, Redis Sorted Sets, or custom timestamp buffers.

Elasticsearch is the source of truth for time-window evaluation.

## 3. Package Structure
Create the following structure under `com.vdt.log_monitor.alert`:
```text
alert/
├── config/
│   └── AlertRuleLoader.java
├── dto/
│   ├── AlertRule.java
│   ├── AlertRuleConfig.java
│   ├── AlertEvent.java
│   └── AlertSeverity.java (Enum: LOW, MEDIUM, HIGH, CRITICAL)
├── repository/
│   └── AlertQueryRepository.java
├── service/
│   ├── AlertCooldownService.java
│   ├── AlertEvaluationService.java
│   └── AlertWebSocketPublisher.java
└── scheduler/
    └── AlertScheduler.java

```

## 4. Domain Models & Configuration (DTOs)

**`rule.json` Format (Place in `src/main/resources`):**

```json
{
  "rules": [
    {
      "id": "HIGH_ERROR_RATE",
      "type": "COUNT_THRESHOLD",
      "enabled": true,
      "environment": "*",
      "application": "*",
      "level": "ERROR",
      "keyword": null,
      "threshold": 10,
      "windowMinutes": 5,
      "cooldownMinutes": 10,
      "severity": "HIGH"
    }
  ]
}

```

**DTO Requirements (Use Lombok for boilerplate):**

* `AlertRule`: String id, AlertRuleType type, boolean enabled, String environment, String application, String level, String keyword, long threshold, long windowMinutes, long cooldownMinutes, AlertSeverity severity.
* `AlertRuleConfig`: List rules.
* `AlertEvent`: String ruleId, String title, String message, String environment, String application, String severity, long matchedCount, Instant triggeredAt.
* `AlertRuleType`: Enum containing `COUNT_THRESHOLD`.

## 5. Component Specifications

### 5.1. AlertRuleLoader

* **Responsibility:** Load and cache `rule.json` at application startup.
* **Logic:** Use Jackson to deserialize. Expose `List<AlertRule> getRules()`.
* **Constraint:** Throw an application startup exception if the JSON file is missing or invalid.

### 5.2. AlertQueryRepository

* **Responsibility:** Query Elasticsearch directly to get the count of matching logs.
* **Important:** Before implementing queries, inspect `common.entity.LogDocument` and use the actual Elasticsearch field names defined there. DO NOT assume field names such as `application`, `environment`, `message`, or `timestamp` without verification.
* **Logic:** Implement `long countMatchingLogs(AlertRule rule)`.
* **Constraint:** Use Spring Data Elasticsearch's `ElasticsearchOperations`. **DO NOT retrieve documents.** Use the Elasticsearch Count API (`NativeQuery` with `count`) for performance.
* **Filters to apply:**
* Timestamp: `>= (now - rule.windowMinutes)`
* Environment: exact match (ignore if `*`)
* Application: exact match (ignore if `*`)
* Level: exact match
* Keyword: match phrase (ignore if null/empty)

* **Sliding Window Rule:** The timestamp filter represents a sliding window query and must be implemented using:

timestamp >= now - windowMinutes

The window is evaluated dynamically on every scheduler execution.

### 5.3. AlertCooldownService

* **Responsibility:** Prevent alert spam (Alert Fatigue) per rule.
* **Logic:** Maintain a thread-safe cache (`ConcurrentHashMap<String, Instant>`) mapping `ruleId` to `lastTriggerTime`.
* **Cache Key:** Use `ruleId` as the unique key for cooldown tracking.
* **Methods:** - `boolean canTrigger(AlertRule rule)`: Returns true if `lastTriggerTime + cooldownMinutes <= now` or if it has never been triggered.
* `void markTriggered(AlertRule rule)`: Updates the timestamp.



### 5.4. AlertWebSocketPublisher

* **Responsibility:** Push alert payloads to the frontend.
* **Logic:** Use `SimpMessagingTemplate`. Method: `publish(AlertEvent event)`.
* **Destination:** `/topic/alerts`.

### 5.5. AlertEvaluationService (Core Logic)

* **Responsibility:** Orchestrate the evaluation process.
* **Logic (`evaluateRules()`):**
1. Iterate over all cached rules. Skip if `!enabled`.
2. Call repository to count matching logs.
3. If `count < threshold`, continue to next rule.
4. If threshold exceeded, check `AlertCooldownService.canTrigger()`.
5. If cooldown is active, continue.
6. If safe to trigger: Create `AlertEvent`, call WebSocket publisher, update cooldown state, and log the event.


* **Message format:** "Detected {matchedCount} {level} logs during the last {windowMinutes} minutes."

### 5.6. AlertScheduler

* **Responsibility:** Trigger the evaluation periodically.
* **Logic:** Use `@Scheduled(fixedDelay = 30000)`.

The next execution starts 30 seconds after the previous execution finishes.

Call `alertEvaluationService.evaluateRules()`.

Ensure `@EnableScheduling` is present in the application config.

## 6. Technical Constraints (Strict)

* **DO NOT** modify the `collector` package, existing query APIs, or existing WebSocket log streaming logic.
* **DO NOT** introduce external dependencies like Redis, RabbitMQ, Kafka, or SQL Database tables.
* **USE ONLY:** Spring Boot core features, Jackson, `ElasticsearchOperations`, `SimpMessagingTemplate`, and Java `java.util.concurrent` collections.

## 7. Testing & Acceptance Criteria

* [ ] `rule.json` is successfully loaded into memory at startup.
* [ ] Scheduler runs periodically using Spring Scheduling with `@Scheduled(fixedDelay = 30000)`.
* [ ] ES is queried using the efficient Count API (no document payload downloaded).
* [ ] Cooldown mechanism strictly prevents duplicate alerts within the specified window.
* [ ] Alerts are correctly formatted and published to `/topic/alerts`.
* [ ] Unit tests are provided for `AlertCooldownService`, `AlertEvaluationService`, and `AlertRuleLoader` (mocking `ElasticsearchOperations`).
* [ ] **Crucial:** The original real-time log streaming from Logstash to the frontend remains untouched and fully functional.
