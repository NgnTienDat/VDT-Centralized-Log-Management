# 3. Error Spike

Ví dụ nhiều hơn 5 lỗi trong 2 phút.

```json
{
  "name": "Error Spike",
  "intervalMinutes": 1,
  "isActive": true,
  "repeatIntervalMinutes": 3,
  "triggerStepId": "check_spike",
  "pipelineSteps": [
    {
      "id": "fetch_errors",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "level:ERROR",
        "metricType": "COUNT",
        "lookBackMinutes": 2,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "check_spike",
      "type": "EVALUATE_THRESHOLD",
      "params": {
        "input": "fetch_errors",
        "operator": "GREATER_THAN",
        "value": 5
      }
    }
  ],
  "notificationTemplate": {
    "title": "Error Spike",
    "message": "Số lượng ERROR/FATAL vượt quá 5 trong 2 phút."
  }
}
```
---

# 6. Error Rate (>5%)

```json
{
  "name": "High Error Rate",
  "intervalMinutes": 1,
  "isActive": true,
  "repeatIntervalMinutes": 3,
  "triggerStepId": "check_error_rate",
  "pipelineSteps": [
    {
      "id": "A",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "query": "level:(ERROR OR FATAL)",
        "metricType": "COUNT",
        "lookBackMinutes": 2,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "B",
      "type": "FETCH_ES_DATA",
      "params": {
        "index": "sys-logs-*",
        "metricType": "COUNT",
        "lookBackMinutes": 2,
        "timeField": "@timestamp"
      }
    },
    {
      "id": "C",
      "type": "MATH",
      "params": {
        "input": [
          "A",
          "fetch_total"
        ],
        "expression": "(#A / #B) * 100"
      }
    },
    {
      "id": "D",
      "type": "EVALUATE_THRESHOLD",
      "params": {
        "input": "C",
        "operator": "GREATER_THAN",
        "value": 5
      }
    }
  ],
  "notificationTemplate": {
    "title": "High Error Rate",
    "message": ""
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
