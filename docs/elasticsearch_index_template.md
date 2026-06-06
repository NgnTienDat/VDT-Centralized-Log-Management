# Elasticsearch Index Template Setup Guide

## Mục tiêu

Đảm bảo tất cả index `sys-logs-*` được tạo với mapping chuẩn ngay từ đầu, tránh Elasticsearch sử dụng Dynamic Mapping gây ra các vấn đề:

* Trường filter bị tạo thành `text` thay vì `keyword`
* Phải truy vấn bằng `.keyword`
* Mapping không nhất quán giữa các index
* Khó mở rộng khi triển khai production

---

# 1. Khởi động Elasticsearch và Logstash

Chỉ khởi động Elasticsearch và Logstash trước.

```bash
docker compose up -d elasticsearch
docker compose up -d logstash
```

Hoặc:

```bash
docker start log-monitor-elasticsearch
docker start log-monitor-logstash
```

Kiểm tra Elasticsearch đã hoạt động:

```bash
curl http://localhost:9200
```

Kết quả mong muốn:

```json
{
  "name": "...",
  "cluster_name": "docker-cluster",
  "version": {
    "number": "9.x.x"
  }
}
```

---

# 2. Tạo Index Template

Tạo template trước khi Filebeat gửi bất kỳ log nào.

```http
PUT _index_template/sys-logs-template
{
  "index_patterns": ["sys-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0
    },
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        },

        "environment": {
          "type": "keyword"
        },

        "app_name": {
          "type": "keyword"
        },

        "host_name": {
          "type": "keyword"
        },

        "log_level": {
          "type": "keyword"
        },

        "logger": {
          "type": "keyword"
        },

        "thread": {
          "type": "keyword"
        },

        "log_message": {
          "type": "text"
        }
      }
    }
  }
}
```

---

# 3. Kiểm tra Template

```http
GET _index_template/sys-logs-template
```

Xác nhận:

```json
{
  "index_patterns": ["sys-logs-*"]
}
```

và các field mong muốn đã được khai báo.

---

# 4. Khởi động Filebeat

Sau khi template đã tồn tại mới khởi động Filebeat.

```bash
docker compose up -d filebeat
```

hoặc

```bash
docker start log-monitor-filebeat
```

---

# 5. Sinh log thử nghiệm

Tạo một vài log từ ứng dụng:

```java
log.info("Test INFO");
log.error("Test ERROR");
```

---

# 6. Kiểm tra index được tạo

```http
GET _cat/indices?v
```

Ví dụ:

```text
sys-logs-dev-2026.06.05
```

---

# 7. Kiểm tra mapping thực tế

```http
GET sys-logs-*/_mapping
```

Kết quả mong muốn:

```json
{
  "environment": {
    "type": "keyword"
  },
  "app_name": {
    "type": "keyword"
  },
  "log_level": {
    "type": "keyword"
  },
  "host_name": {
    "type": "keyword"
  }
}
```

Không còn:

```json
{
  "type": "text",
  "fields": {
    "keyword": {
      "type": "keyword"
    }
  }
}
```

đối với các trường filter chính.

---

# Trường hợp đã tồn tại index sai mapping

Ví dụ index được tạo trước khi template tồn tại.

Mapping hiện tại:

```json
{
  "environment": {
    "type": "text",
    "fields": {
      "keyword": {
        "type": "keyword"
      }
    }
  }
}
```

Khi đó Elasticsearch sẽ không tự cập nhật mapping của index cũ.

---

## Bước 1: Dừng Filebeat

Ngăn log mới tiếp tục được gửi.

```bash
docker stop log-monitor-filebeat
```

---

## Bước 2: Xóa index cũ

Kiểm tra danh sách index:

```http
GET _cat/indices?v
```

Xóa toàn bộ index log:

```http
DELETE sys-logs-*
```

Hoặc xóa một index cụ thể:

```http
DELETE sys-logs-dev-2026.06.05
```

---

## Bước 3: Cập nhật hoặc tạo lại Template

```http
PUT _index_template/sys-logs-template
{
  ...
}
```

---

## Bước 4: Kiểm tra lại Template

```http
GET _index_template/sys-logs-template
```

---

## Bước 5: Khởi động lại Filebeat

```bash
docker start log-monitor-filebeat
```

---

## Bước 6: Sinh log mới

Tạo log mới từ ứng dụng.

Elasticsearch sẽ tạo lại index mới theo template vừa cấu hình.

---

## Bước 7: Kiểm tra mapping

```http
GET sys-logs-*/_mapping
```

Đảm bảo các field:

```json
{
  "environment": {
    "type": "keyword"
  },
  "app_name": {
    "type": "keyword"
  },
  "log_level": {
    "type": "keyword"
  }
}
```

đã đúng như thiết kế.

---

# Checklist triển khai

* [ ] Elasticsearch đã chạy
* [ ] Logstash đã chạy
* [ ] Đã tạo `sys-logs-template`
* [ ] Đã kiểm tra template
* [ ] Chưa khởi động Filebeat
* [ ] Đã khởi động Filebeat sau khi tạo template
* [ ] Đã sinh log thử nghiệm
* [ ] Đã kiểm tra index mới
* [ ] Đã kiểm tra mapping
* [ ] Các trường filter sử dụng kiểu `keyword`
* [ ] Không còn phụ thuộc vào `.keyword` trong Query Service
