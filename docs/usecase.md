Dưới góc nhìn use case, **Kafka, Redis, Elasticsearch, Logstash** là thành phần nội bộ của hệ thống, không nên xem là actor chính. Actor là các bên bên ngoài tương tác với hệ thống.

**Các Actors Chính**

| Actor | Vai trò |
|---|---|
| **Log Producer** | Các hệ thống như Auth, Chat, Payment, Notification gửi log vào hệ thống qua API |
| **Kỹ sư vận hành / App Engineer** | Theo dõi log realtime, tìm kiếm log, nhận cảnh báo, chỉ xem log thuộc ứng dụng mình quản lý |
| **System Admin** | Quản trị toàn hệ thống, cấu hình quyền, ứng dụng, ngưỡng cảnh báo, retention policy |
| **Telegram / Email Gateway** | Nhận yêu cầu gửi cảnh báo từ hệ thống |

**Các Use Case Chính**

| Nhóm | Use case |
|---|---|
| **Tiếp nhận log** | Gửi log đơn lẻ |
|  | Gửi batch log |
|  | Xác thực Producer Service |
|  | Kiểm tra schema log: `Application_Name`, `Log_Level`, `Message`, `Timestamp`, `Trace_ID` |
|  | Ghi log thô vào Message Queue |
|  | Áp dụng rate limiting |
| **Xử lý log** | Consume log thô từ Message Queue |
|  | Parse và chuẩn hóa log |
|  | Gắn trạng thái xử lý: Raw, Normalized, Stored |
|  | Lưu log vào Elasticsearch / DB |
|  | Phát hiện log `ERROR` hoặc `CRITICAL` |
|  | Đẩy event lỗi vào hàng đợi cảnh báo ưu tiên |
| **Cảnh báo** | Consume alert event |
|  | Kiểm tra trùng cảnh báo bằng Redis |
|  | Tạo khóa deduplication theo app, level, message hoặc fingerprint |
|  | Gửi cảnh báo realtime qua WebSocket |
|  | Gửi cảnh báo Telegram |
|  | Gửi cảnh báo Email |
| **Giám sát realtime** | Xem live stream log |
|  | Lọc log theo application |
|  | Lọc log theo level |
|  | Xem chi tiết log theo `Trace_ID` |
|  | Tìm kiếm log theo từ khóa |
| **Phân quyền** | Đăng nhập |
|  | Phân quyền theo vai trò Admin / Engineer |
|  | Giới hạn Engineer chỉ xem log của ứng dụng được cấp quyền |
|  | Admin cấu hình quyền truy cập ứng dụng |
| **Quản trị hệ thống** | Admin cấu hình danh sách application |
|  | Admin cấu hình ngưỡng cảnh báo |
|  | Admin cấu hình retention policy |
|  | Admin theo dõi trạng thái ingestion / processing |
| **Báo cáo & phân tích** | Thống kê số lượng log theo thời gian |
|  | Thống kê tỷ lệ lỗi theo application |
|  | Vẽ biểu đồ health analytics theo giờ |
|  | Xác định ứng dụng có tỷ lệ lỗi cao |
| **Bảo trì dữ liệu** | Tự động xóa log `INFO` quá 7 ngày |
|  | Nén hoặc archive log cũ |
|  | Quản lý vòng đời index |
| **Demo / kiểm thử tải** | Giả lập gửi 500 log trong 2 giây |
|  | Kiểm tra hệ thống tiếp nhận không lỗi |
|  | Kiểm tra dashboard hiển thị log realtime mượt |

Có thể gom lại thành các use case lõi nhất cho sơ đồ Use Case:

1. **Submit Logs**
2. **Process and Normalize Logs**
3. **Store Logs**
4. **Detect Critical Logs**
5. **Deduplicate Alerts**
6. **Send Notifications**
7. **View Realtime Logs**
8. **Search and Filter Logs**
9. **Manage Access Control**
10. **Configure Alert Rules**
11. **View Health Analytics**
12. **Run Log Retention Job**