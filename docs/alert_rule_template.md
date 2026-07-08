# Alert Rule Templates — 10 Rule JSON Sẵn Sàng Chạy

> Tất cả JSON dưới đây đều tương thích với `POST /api/v1/alerts/rules`.
> Chỉ cần thay `index` cho đúng index pattern của hệ thống và điều chỉnh ngưỡng `value` phù hợp.

---

## 🟢 Tầng 1: Rule Cơ Bản

---

### Rule 1 — Error Spike

**Mục đích:** Phát hiện số log ERROR/FATAL tăng đột biến vượt ngưỡng trong 5 phút gần nhất.

```json
{
  "name": "Error Spike",
  "intervalMinutes": 5,
  "isActive": true,
  "repeatIntervalMinutes": 15,
  "triggerStepId": "check_threshold",
  "pipelineSteps": [
    {
      "id": "fetch_errors",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "level:ERROR OR level:FATAL",
        "metricType": "COUNT",
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
    "message": "Tổng số log ERROR/FATAL đã vượt quá 10 trong 5 phút gần nhất."
  }
}
```

---

### Rule 2 — Error By Service

**Mục đích:** Giống Rule 1 nhưng group theo `service.name` để biết chính xác service nào gây lỗi.

```json
{
  "name": "Error By Service",
  "intervalMinutes": 5,
  "isActive": true,
  "repeatIntervalMinutes": 15,
  "triggerStepId": "check_threshold",
  "pipelineSteps": [
    {
      "id": "fetch_errors_by_service",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "level:ERROR OR level:FATAL",
        "metricType": "COUNT",
        "groupBy": ["service"],
        "lookBackMinutes": 5,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "check_threshold",
      "type": "EVALUATE_THRESHOLD",
      "params": {
        "input": "fetch_errors_by_service",
        "operator": "GREATER_THAN",
        "value": 5
      }
    }
  ],
  "notificationTemplate": {
    "title": "🚨 Error Spike By Service",
    "message": "Một hoặc nhiều service có hơn 10 log ERROR/FATAL trong 5 phút qua. Kiểm tra breachedGroups để biết service cụ thể."
  }
}
```

---

### Rule 3 — Exception Detection

**Mục đích:** Phát hiện stack trace / exception ẩn trong log mà tester không thấy qua UI.

```json
{
  "name": "Exception Detection",
  "intervalMinutes": 5,
  "isActive": true,
  "repeatIntervalMinutes": 30,
  "triggerStepId": "check_threshold",
  "pipelineSteps": [
    {
      "id": "fetch_exceptions",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "message:(*Exception* OR *StackOverflow* OR *NullPointer* OR *OutOfMemory*)",
        "metricType": "COUNT",
        "groupBy": ["service"],
        "lookBackMinutes": 10,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "check_threshold",
      "type": "EVALUATE_THRESHOLD",
      "params": {
        "input": "fetch_exceptions",
        "operator": "GREATER_THAN",
        "value": 0
      }
    }
  ],
  "notificationTemplate": {
    "title": "Exception Detected",
    "message": "Phát hiện Exception/StackTrace trong log của service trong 10 phút qua. Có thể là bug ẩn mà tester không thấy qua UI."
  }
}
```

---

### Rule 4 — HTTP 5xx Surge

**Mục đích:** Phát hiện số request HTTP trả về mã 5xx vượt ngưỡng, group theo service.

```json
{
  "name": "HTTP 5xx Surge",
  "intervalMinutes": 5,
  "isActive": true,
  "repeatIntervalMinutes": 15,
  "triggerStepId": "check_threshold",
  "pipelineSteps": [
    {
      "id": "fetch_5xx",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "status_code:[500 TO 599]",
        "metricType": "COUNT",
        "groupBy": ["service"],
        "lookBackMinutes": 5,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "check_threshold",
      "type": "EVALUATE_THRESHOLD",
      "params": {
        "input": "fetch_5xx",
        "operator": "GREATER_THAN",
        "value": 10
      }
    }
  ],
  "notificationTemplate": {
    "title": "HTTP 5xx Surge",
    "message": "Service trả về hơn 10 response HTTP 5xx trong 5 phút qua. Kiểm tra API có bị lỗi."
  }
}
```

---

### Rule 5 — Slow Response

**Mục đích:** Phát hiện response time trung bình vượt ngưỡng cho phép (ví dụ > 3 giây).

```json
{
  "name": "Slow Response",
  "intervalMinutes": 5,
  "isActive": true,
  "repeatIntervalMinutes": 15,
  "triggerStepId": "check_threshold",
  "pipelineSteps": [
    {
      "id": "fetch_response_time",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "metricType": "AVG",
        "metricField": "response_time",
        "groupBy": ["service"],
        "lookBackMinutes": 5,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "check_threshold",
      "type": "EVALUATE_THRESHOLD",
      "params": {
        "input": "fetch_response_time",
        "operator": "GREATER_THAN",
        "value": 3000
      }
    }
  ],
  "notificationTemplate": {
    "title": "Slow Response Detected",
    "message": "Response time trung bình của service vượt quá 3000ms trong 5 phút qua. Có thể service bị quá tải hoặc query chậm."
  }
}
```

---

## 🟡 Tầng 2: Rule Trung Bình

---

### Rule 6 — Error Rate %

**Mục đích:** Tính tỷ lệ lỗi = (số log error / tổng log) × 100%. Cảnh báo nếu > 5%.

```json
{
  "name": "Error Rate Percentage",
  "intervalMinutes": 5,
  "isActive": true,
  "repeatIntervalMinutes": 15,
  "triggerStepId": "calculate_error_rate",
  "pipelineSteps": [
    {
      "id": "fetch_error_count",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "level:ERROR OR level:FATAL",
        "metricType": "COUNT",
        "lookBackMinutes": 10,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "fetch_total_count",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "metricType": "COUNT",
        "lookBackMinutes": 10,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "calculate_error_rate",
      "type": "MATH",
      "params": {
        "input": ["fetch_error_count", "fetch_total_count"],
        "expression": "#fetch_total_count > 0 ? (#fetch_error_count / #fetch_total_count) > 0.05 : false"
      }
    }
  ],
  "notificationTemplate": {
    "title": "Error Rate Exceeded 5%",
    "message": "Tỷ lệ log ERROR/FATAL vượt quá 5% tổng số log trong 10 phút qua. Chất lượng service đang suy giảm."
  }
}
```

---

### Rule 7 — Log Silence / No Heartbeat

**Mục đích:** Phát hiện service ngừng ghi log đột ngột (service có thể đã chết hoặc treo).

```json
{
  "name": "Log Silence - No Heartbeat",
  "intervalMinutes": 5,
  "isActive": true,
  "repeatIntervalMinutes": 30,
  "triggerStepId": "check_silence",
  "pipelineSteps": [
    {
      "id": "fetch_log_count",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "metricType": "COUNT",
        "groupBy": ["service"],
        "lookBackMinutes": 10,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "check_silence",
      "type": "EVALUATE_THRESHOLD",
      "params": {
        "input": "fetch_log_count",
        "operator": "EQUALS",
        "value": 0
      }
    }
  ],
  "notificationTemplate": {
    "title": "Log Silence Detected",
    "message": "Một hoặc nhiều service không ghi bất kỳ log nào trong 10 phút qua. Service có thể đã chết hoặc bị treo."
  }
}
```

---

### Rule 8 — Memory/CPU Warning

**Mục đích:** Phát hiện mức sử dụng bộ nhớ (hoặc CPU) đạt ngưỡng nguy hiểm dựa trên metric field trong log.

```json
{
  "name": "Memory Usage Warning",
  "intervalMinutes": 5,
  "isActive": true,
  "repeatIntervalMinutes": 15,
  "triggerStepId": "check_threshold",
  "pipelineSteps": [
    {
      "id": "fetch_memory_usage",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-metrics-*",
        "metricType": "MAX",
        "metricField": "memory_usage_mb",
        "groupBy": ["service"],
        "lookBackMinutes": 5,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "check_threshold",
      "type": "EVALUATE_THRESHOLD",
      "params": {
        "input": "fetch_memory_usage",
        "operator": "GREATER_THAN",
        "value": 1024
      }
    }
  ],
  "notificationTemplate": {
    "title": "Memory Usage Warning",
    "message": "Service sử dụng hơn 1024 MB bộ nhớ. Có nguy cơ OutOfMemoryError."
  }
}
```

---

## 🔴 Tầng 3: Rule Nâng Cao

---

### Rule 9 — Cross-Service Error Correlation

**Mục đích:** Phát hiện khi cả 2 service có liên quan (ví dụ auth-service và payment-service) cùng xảy ra lỗi đồng thời — dấu hiệu của lỗi lan truyền (cascading failure).

> **Lưu ý:** Thay `auth-service` và `payment-service` bằng tên service thực tế trong hệ thống.

```json
{
  "name": "Cross-Service Error Correlation",
  "intervalMinutes": 5,
  "isActive": true,
  "repeatIntervalMinutes": 30,
  "triggerStepId": "check_correlation",
  "pipelineSteps": [
    {
      "id": "fetch_service_a_errors",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "level:ERROR AND service:auth-service",
        "metricType": "COUNT",
        "lookBackMinutes": 5,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "fetch_service_b_errors",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "level:ERROR AND service:payment-service",
        "metricType": "COUNT",
        "lookBackMinutes": 5,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "check_correlation",
      "type": "MATH",
      "params": {
        "input": ["fetch_service_a_errors", "fetch_service_b_errors"],
        "expression": "#fetch_service_a_errors > 0 && #fetch_service_b_errors > 0"
      }
    }
  ],
  "notificationTemplate": {
    "title": "Cross-Service Error Correlation",
    "message": "Cả auth-service và payment-service cùng phát sinh lỗi trong 5 phút qua. Có thể là lỗi lan truyền giữa các service."
  }
}
```

---

### Rule 10 — Error Storm (Đột Biến Gấp Bội)

**Mục đích:** So sánh số lượng error trong 5 phút gần nhất với baseline (trung bình 30 phút). Nếu gấp 3 lần → đang có Error Storm.

```json
{
  "name": "Error Storm Detection",
  "intervalMinutes": 5,
  "isActive": true,
  "repeatIntervalMinutes": 15,
  "triggerStepId": "check_storm",
  "pipelineSteps": [
    {
      "id": "fetch_recent_errors",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "level:ERROR OR level:FATAL",
        "metricType": "COUNT",
        "lookBackMinutes": 5,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "fetch_baseline_errors",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "level:ERROR OR level:FATAL",
        "metricType": "COUNT",
        "lookBackMinutes": 30,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "check_storm",
      "type": "MATH",
      "params": {
        "input": ["fetch_recent_errors", "fetch_baseline_errors"],
        "expression": "(#fetch_baseline_errors / 6) > 0 ? (#fetch_recent_errors / (#fetch_baseline_errors / 6)) > 3 : #fetch_recent_errors > 5"
      }
    }
  ],
  "notificationTemplate": {
    "title": "Error Storm Detected",
    "message": "Số lượng ERROR trong 5 phút gần nhất gấp hơn 3 lần mức trung bình 5-phút (tính từ baseline 30 phút). Hệ thống đang trong tình trạng bất thường."
  }
}
```

---

## Hướng Dẫn Sử Dụng

### Gửi request tạo rule

```bash
curl -X POST http://localhost:8080/api/v1/alerts/rules \
  -H "Content-Type: application/json" \
  -d '<PASTE_JSON_Ở_TRÊN>'
```

### Tùy chỉnh trước khi dùng

| Tham số | Ý nghĩa | Cần thay đổi? |
|---------|----------|:--------------:|
| `index` | Index pattern của Elasticsearch | ✅ Bắt buộc — thay bằng index thực tế |
| `query` | Lucene query string | ⚠️ Tuỳ — điều chỉnh tên field cho khớp mapping |
| `groupBy` | Field để phân nhóm kết quả | ⚠️ Tuỳ — đảm bảo field là keyword |
| `metricField` | Field số để tính toán (SUM/AVG/MAX/MIN) | ⚠️ Tuỳ — chỉ cần nếu metricType ≠ COUNT |
| `value` (threshold) | Ngưỡng cảnh báo | ✅ Bắt buộc — điều chỉnh theo thực tế |
| `lookBackMinutes` | Khoảng thời gian nhìn lại | Tuỳ chọn (mặc định 5) |
| `intervalMinutes` | Chu kỳ chạy rule | Tuỳ chọn |
| `repeatIntervalMinutes` | Cooldown giữa các lần gửi cảnh báo | Tuỳ chọn |
| `timeField` | Tên field timestamp trong ES | Tuỳ chọn (mặc định `@timestamp`) |

### Thứ tự triển khai khuyến nghị

1. **Bắt đầu bằng Rule 1 (Error Spike)** — đơn giản nhất, dễ kiểm chứng
2. **Thêm Rule 3 (Exception Detection)** — giá trị cao nhất cho mục tiêu phát hiện bug ẩn
3. **Thêm Rule 2 (Error By Service)** — cụ thể hoá service nào lỗi
4. **Thêm Rule 7 (Log Silence)** — phát hiện service chết
5. **Các rule còn lại** — bổ sung dần khi hệ thống ổn định


