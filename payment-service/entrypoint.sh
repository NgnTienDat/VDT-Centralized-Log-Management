#!/bin/sh
set -e

# Chown lại thư mục logs mỗi lần container start (vì volume mount có thể đè quyền)
chown -R appuser:appgroup /app/logs

# Chạy app bằng appuser thay vì root
exec su-exec appuser java -jar app.jar