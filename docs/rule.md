# 1. Log ERROR/FATAL xuất hiện

```json
{
  "name": "Error/Fatal Detected",
  "intervalMinutes": 1,
  "isActive": true,
  "repeatIntervalMinutes": 10,
  "triggerStepId": "check_error",
  "pipelineSteps": [
    {
      "id": "fetch_errors",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "level:(ERROR OR FATAL)",
        "metricType": "COUNT",
        "lookBackMinutes": 1,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "check_error",
      "type": "EVALUATE_THRESHOLD",
      "params": {
        "input": "fetch_errors",
        "operator": "GREATER_THAN",
        "value": 0
      }
    }
  ],
  "notificationTemplate": {
    "title": "🚨 Error/Fatal Detected",
    "message": "Phát hiện log ERROR hoặc FATAL trong 1 phút gần nhất."
  }
}
```

---

# 2. Exception Detection

```json
{
  "name": "Critical Exception Detection",
  "intervalMinutes": 2,
  "isActive": true,
  "repeatIntervalMinutes": 15,
  "triggerStepId": "check_exception",
  "pipelineSteps": [
    {
      "id": "fetch_exception",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "message:(NullPointerException OR SQLException OR TimeoutException OR OutOfMemoryError)",
        "metricType": "COUNT",
        "groupBy": [
          "service"
        ],
        "lookBackMinutes": 5,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "check_exception",
      "type": "EVALUATE_THRESHOLD",
      "params": {
        "input": "fetch_exception",
        "operator": "GREATER_THAN",
        "value": 0
      }
    }
  ],
  "notificationTemplate": {
    "title": "Critical Exception Detected",
    "message": "Phát hiện Exception nghiêm trọng trong log."
  }
}
```

---

# 3. Error Spike

Ví dụ nhiều hơn 20 lỗi trong 5 phút.

```json
{
  "name": "Error Spike",
  "intervalMinutes": 5,
  "isActive": true,
  "repeatIntervalMinutes": 15,
  "triggerStepId": "check_spike",
  "pipelineSteps": [
    {
      "id": "fetch_errors",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "level:(ERROR OR FATAL)",
        "metricType": "COUNT",
        "lookBackMinutes": 5,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "check_spike",
      "type": "EVALUATE_THRESHOLD",
      "params": {
        "input": "fetch_errors",
        "operator": "GREATER_THAN",
        "value": 20
      }
    }
  ],
  "notificationTemplate": {
    "title": "Error Spike",
    "message": "Số lượng ERROR/FATAL vượt quá 20 trong 5 phút."
  }
}
```

---

# 4. Business Error

Không dùng wildcard nữa.

```json
{
  "name": "Business Error",
  "intervalMinutes": 2,
  "isActive": true,
  "repeatIntervalMinutes": 15,
  "triggerStepId": "check_business",
  "pipelineSteps": [
    {
      "id": "fetch_business",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "message:(\"Payment failed\" OR \"Order rollback\" OR \"Send email failed\" OR \"OTP expired\")",
        "metricType": "COUNT",
        "groupBy": [
          "service"
        ],
        "lookBackMinutes": 5,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "check_business",
      "type": "EVALUATE_THRESHOLD",
      "params": {
        "input": "fetch_business",
        "operator": "GREATER_THAN",
        "value": 0
      }
    }
  ],
  "notificationTemplate": {
    "title": "Business Error",
    "message": "Phát hiện Business Error."
  }
}
```

---

# 5. Keyword Detection

```json
{
  "name": "Keyword Detection",
  "intervalMinutes": 2,
  "isActive": true,
  "repeatIntervalMinutes": 15,
  "triggerStepId": "check_keyword",
  "pipelineSteps": [
    {
      "id": "fetch_keyword",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "message:(timeout OR deadlock OR \"connection refused\" OR \"broken pipe\")",
        "metricType": "COUNT",
        "groupBy": [
          "service"
        ],
        "lookBackMinutes": 5,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "check_keyword",
      "type": "EVALUATE_THRESHOLD",
      "params": {
        "input": "fetch_keyword",
        "operator": "GREATER_THAN",
        "value": 0
      }
    }
  ],
  "notificationTemplate": {
    "title": "Keyword Detected",
    "message": "Phát hiện keyword lỗi trong log."
  }
}
```

---

# 6. Error Rate (>5%)

Đây là rule mình chỉnh nhiều nhất để đúng với `MathExecutor`.

## Step 1

Đếm Error

## Step 2

Đếm Total

## Step 3

Tính %

## Step 4

So sánh ngưỡng

```json
{
  "name": "High Error Rate",
  "intervalMinutes": 5,
  "isActive": true,
  "repeatIntervalMinutes": 15,
  "triggerStepId": "check_error_rate",
  "pipelineSteps": [
    {
      "id": "fetch_error",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "level:(ERROR OR FATAL)",
        "metricType": "COUNT",
        "lookBackMinutes": 5,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "fetch_total",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "metricType": "COUNT",
        "lookBackMinutes": 5,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "calculate_error_rate",
      "type": "MATH",
      "params": {
        "input": [
          "fetch_error",
          "fetch_total"
        ],
        "expression": "#fetch_total > 0 ? (#fetch_error / #fetch_total) * 100 : 0"
      }
    },
    {
      "id": "check_error_rate",
      "type": "EVALUATE_THRESHOLD",
      "params": {
        "input": "calculate_error_rate",
        "operator": "GREATER_THAN",
        "value": 5
      }
    }
  ],
  "notificationTemplate": {
    "title": "High Error Rate",
    "message": "Tỷ lệ ERROR/FATAL vượt quá 5% tổng số log trong 5 phút gần nhất."
  }
}
```

---

## Hai góp ý nhỏ sau khi đọc source

Có hai điểm mình nghĩ bạn nên cải thiện trong engine để rule linh hoạt hơn:

1. **Thêm `FETCH_ES_DATA` với `metricType = FILTER_COUNT`**: Cho phép đếm theo điều kiện ngay trong aggregation (ví dụ tính `errorCount` và `totalCount` theo từng `service` trong một query), giúp giảm số lần gọi Elasticsearch.

2. **Hỗ trợ `queryType`**: Hiện executor luôn dùng `query_string`. Có thể mở rộng:

```json
{
  "queryType": "QUERY_STRING"
}
```

hoặc

```json
{
  "queryType": "KQL"
}
```

hoặc

```json
{
  "queryType": "DSL"
}
```

Điều này sẽ giúp hệ thống dễ mở rộng hơn trong tương lai mà không cần sửa `FetchEsDataExecutor`.
