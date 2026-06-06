Bạn là một Chuyên gia Lập trình Backend Java và Hệ thống Phân tán. Hãy truy cập module có tên là `query-service`. 

Dịch vụ này được viết bằng Spring Boot 4.x, có nhiệm vụ kết nối vào Elasticsearch để truy vấn dữ liệu log đã được chuẩn hóa bởi Logstash và trả về cho Frontend hiển thị.

Dưới đây là thông tin chi tiết về cấu trúc dữ liệu log hiện tại trong Elasticsearch và các yêu cầu mã nguồn cần triển khai:

---

### 1. NGỮ CẢNH DỮ LIỆU (LOG STRUCTURE IN ELASTICSEARCH)
Logstash đã parse thành công dữ liệu log thô từ Spring Boot và lưu vào Elasticsearch dưới các index có pattern dạng: `sys-logs-dev-2026.06.03`, `sys-logs-prod-2026.06.04`, v.v. (Đặt chung là `sys-logs-*`).

Một Document mẫu trong Elasticsearch có cấu trúc các trường chính xác như sau (hãy dùng đúng tên trường này để map Entity):
- `@timestamp`: Kiểu Date (ISO8601), thời gian xảy ra log.
- `environment`: Kiểu Keyword, môi trường chạy ứng dụng (ví dụ: dev, staging, prod).
- `app_name`: Kiểu Keyword, tên của microservice sinh ra log (ví dụ: logs-app).
- `log_level`: Kiểu Keyword, cấp độ lỗi (INFO, WARN, ERROR, DEBUG).
- `logger`: Kiểu Text, tên class sinh ra log (ví dụ: com.dev.logsapp.TestController).
- `thread`: Kiểu Keyword, tên thread xử lý (ví dụ: http-nio-8080-exec-1).
- `log_message`: Kiểu Text (Standard Analyzer), chứa nội dung thông điệp log hoặc toàn bộ chuỗi Java Stacktrace lỗi dài.

---

### 2. YÊU CẦU TRIỂN KHAI MÃ NGUỒN (SPRING BOOT 4.x)

Hãy viết code hoàn chỉnh, sạch sẽ theo mô hình Layered Architecture (Controller -> Service -> Repository) cho các phần sau:

#### A. Cấu hình hệ thống (application.yml)
- Chạy service ở port `8081`.
- Cấu hình `spring.elasticsearch.uris` trỏ tới địa chỉ mặc định `http://localhost:9200`.

#### B. Lớp Data Model / Entity (`LogDocument.java`)
- Tạo class entity ánh xạ tới index pattern `sys-logs-*`.
- Sử dụng các Annotation của Spring Data Elasticsearch (`@Document`, `@Id`, `@Field`) để định nghĩa chính xác cấu trúc dữ liệu trên. 
- Đặc biệt chú ý mapping trường `@timestamp` sử dụng `FieldType.Date` và format phù hợp.

#### C. Lớp Repository (`LogRepository.java`)
- Kế thừa từ `ElasticsearchRepository`.
- Viết các hàm tìm kiếm phân trang (Pageable) nâng cao:
  1. Tìm kiếm kết hợp theo các trường chính xác: `environment`, `appName`, `logLevel`.
  2. Tìm kiếm full-text search theo từ khóa xuất hiện trong nội dung của trường `log_message`.

#### D. Lớp Controller (`LogQueryController.java`)
- Tạo các REST API endpoints tại đường dẫn `/api/v1/logs`.
- Hỗ trợ cơ chế phân trang (`page`, `size`) và mặc định luôn sắp xếp (Sort) theo trường `@timestamp` giảm dần (mới nhất lên đầu).

---

### 3. YÊU CẦU ĐẦU RA
- Code phải hoàn chỉnh, không viết tắt, không để lại comment TODO.
- Có xử lý ngoại lệ (Exception Handling) cơ bản nếu không kết nối được tới Elasticsearch.
- Sử dụng Lombok một cách tối ưu để code gọn gàng.
- Mọi response API phải thống nhất theo class ApiResponse của common-core.