# Phân Tích Alert Rules Cho Hệ Thống Log Tập Trung (Dev/Test/Staging)

## 1. Đánh Giá Khả Năng Hiện Tại Của Engine

Dựa trên code hiện tại, engine của bạn đã có một **pipeline khá mạnh** với 3 executor:

| Executor | Chức năng | Khả năng |
|----------|-----------|----------|
| [FETCH_ES_DATA](../log-monitor-backend/src/main/java/com/vdt/log_monitor/alert/engine/FetchEsDataExecutor.java) | Query Elasticsearch | Index pattern, rawQuery (query_string), groupBy (multi-level), metricType (COUNT/SUM/MAX/MIN/AVG), lookBackMinutes, timeField |
| [EVALUATE_THRESHOLD](../log-monitor-backend/src/main/java/com/vdt/log_monitor/alert/engine/EvaluateThresholdExecutor.java) | So sánh ngưỡng | 6 toán tử: `>`, `<`, `>=`, `<=`, `==`, `!=` — hỗ trợ cả dữ liệu phân nhóm |
| [MATH](../log-monitor-backend/src/main/java/com/vdt/log_monitor/alert/engine/MathExecutor.java) | Biểu thức SpEL | Tính toán liên step, hỗ trợ `&&`, `\|\|`, `==`, `!=`, `>`, `<`, phân nhóm song song |

Cộng thêm:
- **Scheduler** chạy theo `intervalMinutes` với cooldown (`repeatIntervalMinutes`) tránh spam
- **State machine** đơn giản: `OK ↔ FIRING`
- **GroupBy multi-level**: phát hiện chính xác *cụm nào* vi phạm (service nào, status code nào...)

---

## 2. Danh Sách Rule Đề Xuất — Phù Hợp Với Dev/Test/Staging

> [!IMPORTANT]
> Đây là môi trường dev/test/staging — mục tiêu là **phát hiện bug ẩn mà tester không thấy qua UI**. Các rule dưới đây đều nằm trong khả năng engine hiện tại, KHÔNG cần mở rộng thêm code.

### 🟢 Tầng 1: Rule Cơ Bản (Bắt buộc, dùng 1 FETCH + 1 THRESHOLD)

| # | Tên Rule | Mô tả | Pipeline |
|---|----------|-------|----------|
| 1 | **Error Spike** | Số log ERROR/FATAL vượt ngưỡng trong N phút | `FETCH_ES_DATA(query:"level:ERROR OR level:FATAL", metricType:COUNT)` → `EVALUATE_THRESHOLD(> N)` |
| 2 | **Error By Service** | Như trên nhưng group theo service name | Thêm `groupBy: ["service.name"]` → biết chính xác service nào lỗi |
| 3 | **Exception Detection** | Phát hiện stack trace / exception trong log | `FETCH_ES_DATA(query:"Exception OR NullPointerException OR StackOverflow", COUNT)` → `THRESHOLD(> 0)` |
| 4 | **HTTP 5xx Surge** | Số request 5xx vượt ngưỡng | `FETCH_ES_DATA(query:"status_code:[500 TO 599]", COUNT, groupBy:["service.name"])` → `THRESHOLD(> 10)` |
| 5 | **Slow Response** | Response time trung bình vượt ngưỡng | `FETCH_ES_DATA(metricType:AVG, metricField:"response_time")` → `THRESHOLD(> 3000)` |

### 🟡 Tầng 2: Rule Trung Bình (Nên có, dùng MATH để kết hợp dữ liệu)

| # | Tên Rule | Mô tả | Pipeline |
|---|----------|-------|----------|
| 6 | **Error Rate %** | Tỷ lệ lỗi = error_count / total_count > X% | `FETCH(error)` + `FETCH(total)` → `MATH(#error_step / #total_step > 0.05)` |
| 7 | **Log Silence / No Heartbeat** | Một service đột ngột không ghi log (service chết?) | `FETCH_ES_DATA(groupBy:["service.name"], COUNT)` → `THRESHOLD(< 1)` hoặc `EQUALS 0` |
| 8 | **Memory/CPU Warning** | Nếu log có chứa metric field | `FETCH(metricType:MAX, metricField:"memory_usage_mb")` → `THRESHOLD(> 1024)` |

### 🔴 Tầng 3: Rule Nâng Cao (Tùy chọn, giá trị cao cho staging)

| # | Tên Rule | Mô tả | Pipeline |
|---|----------|-------|----------|
| 9 | **Cross-Service Error Correlation** | Khi cả service A lẫn B cùng lỗi | `FETCH(service A errors)` + `FETCH(service B errors)` → `MATH(#stepA > 0 && #stepB > 0)` |
| 10 | **Error Storm** | Error count tăng đột biến gấp N lần so với bình thường | `FETCH(5m)` + `FETCH(30m, chia cho 6)` → `MATH(#recent / (#baseline/6) > 3)` |

---

## 3. Đánh Giá Mức Độ Phức Tạp — Engine Này Ở Đâu?

```
┌──────────────────────────────────────────────────────────────────┐
│  Đơn giản                                            Phức tạp    │
│  ←─────────────────────────────────────────────────────────────→ │
│                                                                  │
│  grep+cron    [HỆ THỐNG CỦA BẠN]    Grafana     Datadog/Splunk   │
│     │              │                    │              │         │
│     ▼              ▼                    ▼              ▼         │
│  Text match   Threshold trên       Anomaly        ML-based       │
│  đơn giản     ES aggregation,      detection,     alerting,      │
│               SpEL expression,     PromQL,        AIOps,         │
│               groupBy, cooldown    multi-channel  correlation    │
└──────────────────────────────────────────────────────────────────┘
```

### Hệ thống của bạn đã làm được (✅) và chưa cần làm (❌):

| Tính năng | Trạng thái | Nhận xét |
|-----------|:----------:|----------|
| Threshold alerting (đếm, trung bình, min, max) | ✅ | Đủ dùng |
| GroupBy multi-level (biết cụ thể cụm nào lỗi) | ✅ | Rất tốt |
| Biểu thức toán học / logic liên step | ✅ | SpEL đủ mạnh |
| Cooldown / repeat interval | ✅ | Tránh spam |
| State machine (OK ↔ FIRING) | ✅ | Đủ dùng |
| Anomaly detection (ML) | ❌ | Quá phức tạp, không cần cho dev/test |
| Multi-channel notification (email/Slack/PagerDuty) | ❌ | Có thể thêm đơn giản sau |
| Dashboard / visualization | ❌ | Frontend đã có phần xem log |
| Log correlation / distributed tracing | ❌ | Dành cho Jaeger/Zipkin |
| SLO/SLI monitoring | ❌ | Chỉ cần cho production |

---

## 4. Kết Luận & Khuyến Nghị

> [!TIP]
> **Engine hiện tại của bạn hoàn toàn phù hợp** cho mục tiêu đã đề ra: phát hiện bug ẩn trong dev/test/staging.

### Tại sao KHÔNG cần phức tạp hơn?

1. **Đối tượng sử dụng là developer/tester nội bộ** — họ cần biết "service X có lỗi bất thường" chứ không cần machine learning phát hiện anomaly
2. **Môi trường dev/test/staging** — traffic thấp, pattern không ổn định → anomaly detection sẽ tạo quá nhiều false positive
3. **10 rule ở trên đã phủ 90% use case** thực tế:
   - Error spike → phát hiện crash loop
   - Error by service → định vị nhanh service lỗi  
   - Exception detection → bug ẩn mà UI không thấy
   - HTTP 5xx → API failure
   - Log silence → service chết/hang
   - Error rate → suy giảm chất lượng

### Khi nào nên chuyển sang công cụ lớn hơn?

Chỉ khi:
- Cần monitor **production** với SLA/SLO
- Cần **anomaly detection** dựa trên machine learning
- Cần **distributed tracing** cross-service
- Cần tích hợp **on-call rotation** (PagerDuty, OpsGenie)
- Quản lý **hàng trăm rule** với RBAC phức tạp

> [!NOTE]
> Với hệ thống hiện tại, bạn nên tập trung vào **Tầng 1** (5 rule cơ bản) trước, rồi bổ sung Tầng 2 nếu thấy cần. Tầng 3 là "nice-to-have". Quan trọng nhất là **phần notification** (gửi về Telegram/Slack/WebSocket) và **UI tạo rule** cho user dễ dùng.

---

## 5. JSON Mẫu — Rule "Error Spike By Service" (Copy & Test Ngay)

```json
{
  "name": "Error Spike By Service",
  "intervalMinutes": 5,
  "isActive": true,
  "repeatIntervalMinutes": 15,
  "triggerStepId": "check_threshold",
  "pipelineSteps": [
    {
      "id": "fetch_errors",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "app-logs-*",
        "query": "level:ERROR OR level:FATAL",
        "metricType": "COUNT",
        "groupBy": ["service.name"],
        "lookBackMinutes": 5,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "check_threshold",
      "type": "EVALUATE_THRESHOLD",
      "params": {
        "input": "fetch_errors",
        "operator": "GREATER_THAN",
        "value": 10
      }
    }
  ],
  "notificationTemplate": {
    "title": "🚨 Error Spike Detected",
    "message": "Service có hơn 10 log ERROR/FATAL trong 5 phút qua"
  }
}
```
