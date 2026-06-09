# =============================================================
# FULL FLOW: Logstash → Spring Boot → STOMP WebSocket
# =============================================================

# BƯỚC 0: Filebeat đọc file .log và gửi về Logstash
# ─────────────────────────────────────────────────
#
#   [Spring Boot service]
#       → ghi ra /var/log/auth-service.log
#       → Filebeat agent đọc file (tail -f)
#       → gắn tag: environment=dev, service_name=auth-service
#       → gửi TCP/SSL đến Logstash:5044


# BƯỚC 1: Logstash parse và fan-out output
# ─────────────────────────────────────────
#
#   Logstash nhận beat event
#   │
#   ├─ filter: grok parse → extract level, traceId, message, thread...
#   ├─ filter: date parse → chuẩn hóa @timestamp về UTC+7
#   ├─ filter: mutate    → thêm host_name, bỏ metadata thừa
#   │
#   └─ output (song song):
#       ├─ [1] Elasticsearch ← MỌI log (INFO, WARN, ERROR, DEBUG)
#       │       index: sys-logs-{env}-{date}
#       │       Tất cả log được lưu đầy đủ để query sau
#       │
#       └─ [2] HTTP POST /internal/logs/ingest ← CHỈ ERROR và WARN
#               Chỉ log quan trọng mới đi qua webhook
#               INFO/DEBUG không cần realtime alert


# BƯỚC 2: LogCollectorController nhận webhook
# ────────────────────────────────────────────
#
#   POST /internal/logs/ingest
#   {
#     "traceId":     "a3f9c12b4e8d1027",
#     "level":       "ERROR",
#     "environment": "dev",
#     "service":     "auth-service",
#     "thread":      "http-nio-8080-exec-5",
#     "message":     "NullPointerException at AuthController.java:142",
#     "timestamp":   "2026-06-05T14:44:17.117Z",
#     "hostName":    "auth-service-pod-1"
#   }
#
#   Controller nhận → gọi LogCollectorService.ingest() (@Async)
#   Controller trả về 202 Accepted ngay — không chờ xử lý xong


# BƯỚC 3: LogCollectorService map + publish event
# ─────────────────────────────────────────────────
#
#   LogIngestRequest → map → LogMessageDto
#   applicationEventPublisher.publishEvent(new LogIngestedEvent(dto))
#
#   Spring Event Bus nhận event → gọi tất cả @EventListener song song:


# BƯỚC 4a: LogWebSocketPublisher broadcast STOMP
# ───────────────────────────────────────────────
#
#   Publish đến 3 topic:
#   /topic/logs.dev.error   → Client đang xem DEV + ERROR
#   /topic/logs.dev.all     → Client đang xem tất cả DEV
#   /topic/logs.all         → Client đang xem tất cả
#
#   Client đang subscribe /topic/logs.dev.error
#   → nhận frame → hook useLogStream xử lý
#   → prepend vào displayLogs state
#   → React re-render → log xuất hiện trên UI


# BƯỚC 4b: AlertEvaluator check rule (song song với 4a)
# ──────────────────────────────────────────────────────
#
#   Nhận cùng LogIngestedEvent, chạy trên thread khác
#   Check: auth-service:dev có >= 5 ERROR trong 60 giây?
#   Nếu có → AlertNotifyService.triggerAlert()
#       → Lưu alert vào DB
#       → STOMP publish /topic/alerts → client nhận
#       → Telegram/Email (nếu cấu hình)


# =============================================================
# FLOW DIAGRAM (ASCII)
# =============================================================
#
#  Spring Boot logs
#       │ (file)
#  Filebeat agent ──────────────────────────────────► Logstash:5044
#  [tag: env=dev, service=auth-service]                    │
#                                                     grok parse
#                                                          │
#                                         ┌────────────────┴───────────────┐
#                                         ▼                                ▼
#                               Elasticsearch                  POST /internal/logs/ingest
#                             (tất cả log level)               (chỉ ERROR + WARN)
#                                                                          │
#                                                              LogCollectorController
#                                                              return 202 Accepted
#                                                                          │ @Async
#                                                              LogCollectorService
#                                                              map → LogMessageDto
#                                                              publishEvent()
#                                                                          │
#                                                        ┌─────────────────┴──────────────┐
#                                                        ▼                                ▼
#                                            LogWebSocketPublisher              AlertEvaluator
#                                            broadcast 3 topics                 check threshold
#                                                        │                           │
#                                            STOMP /topic/logs.*          AlertNotifyService
#                                                        │                           │
#                                            Client browser              /topic/alerts + Telegram
#                                            useLogStream hook                       │
#                                                        │                   Client browser
#                                            UI prepend log row            AlertBanner component


# =============================================================
# CHECKLIST triển khai
# =============================================================
#
# Backend:
#   [x] @EnableAsync trên LogMonitorBackendApplication
#   [x] WebSocketConfig — STOMP endpoint + broker
#   [x] LogCollectorController — POST /internal/logs/ingest
#   [x] LogCollectorService    — @Async, map, publishEvent
#   [x] LogIngestedEvent       — Spring ApplicationEvent
#   [x] LogWebSocketPublisher  — @EventListener, broadcast topics
#   [x] AlertEvaluator         — @EventListener, rule engine
#   [x] AlertNotifyService     — trigger alert + push /topic/alerts
#
# Logstash:
#   [x] Output 1: Elasticsearch — tất cả log
#   [x] Output 2: HTTP webhook  — chỉ ERROR + WARN
#   [x] Filter: level in ["ERROR", "WARN"] trước khi gọi HTTP output
#
# Frontend:
#   [x] useLogStream hook — subscribe đúng topic theo filter
#   [x] subscribe /topic/alerts — hiển thị AlertBanner
#   [x] Tách rawBuffer (cho stats) và displayLogs (cho UI)