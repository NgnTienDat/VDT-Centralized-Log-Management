Dưới đây là nội dung đã được chuẩn hóa và cấu trúc lại dưới dạng Markdown, tối ưu hóa cho tài liệu kỹ thuật hoặc báo cáo dự án để bạn dễ dàng theo dõi và bổ sung.
------------------------------
## Đề bài dự án: Hệ thống Giám sát dữ liệu Log và Cảnh báo (Log Monitor & Alert System)
Chương trình: Viettel Digital Talent 2026 — STT: 30
## 1. Mô tả bài toán

* Mục tiêu: Xây dựng hệ thống thu thập Log tập trung, hỗ trợ tìm kiếm nhanh và phát hiện lỗi/phát tin cảnh báo.
* Môi trường sử dụng: Chủ yếu phục vụ môi trường Dev/Test (hiện tại dự án đích có 3 - 4 môi trường chạy).
* Giá trị cốt lõi: Giúp Tester và Dev chủ động kiểm tra hệ thống, cảnh báo sớm các bug tiềm ẩn chạy ngầm bên dưới (mức hệ thống) mà việc test giao diện (UI) thông thường không phát hiện ra.

## 2. Yêu cầu chức năng & Công nghệ

* Log Collector: Thu thập toàn bộ log từ các microservices hiện có gửi về Elasticsearch tập trung.
* Log Viewer Dashboard: Giao diện hiển thị danh sách log, hỗ trợ bộ lọc theo Level (INFO, WARN, ERROR) hoặc theo Service Name.
* Alerting: Tự động phát thông báo/cảnh báo khi xuất hiện lượng lớn log dạng ERROR.
* Tech Stack bắt buộc: Elasticsearch, Logstash, Spring Java.

## 3. Tiêu chí đầu ra (Deliverables)

* Hệ thống chạy được: Đúng tài liệu giải pháp, đảm bảo hiệu năng và sẵn sàng đóng gói để deploy lên Production.
* Tài liệu đi kèm: Tài liệu kiến trúc/giải pháp hệ thống (Architecture/Solution Document) và Hướng dẫn cài đặt, triển khai (Deployment Guide).
