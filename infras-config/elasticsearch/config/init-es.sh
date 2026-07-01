#!/bin/bash
# =============================================================
# init-es.sh — chạy MỘT LẦN sau docker-compose up lần đầu
#
# Cách dùng:
#   chmod +x init-es.sh
#   ./init-es.sh                        # mặc định localhost:9200
#   ./init-es.sh http://10.0.0.5:9200   # chỉ định URL khác
# =============================================================

ES_URL="${1:-http://localhost:9200}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TEMPLATE_FILE="$SCRIPT_DIR/../index-templates/log-template.json"
ILM_FILE="$SCRIPT_DIR/../ilm-policies/logs-ilm.json"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log_ok()   { echo -e "${GREEN}[OK]${NC}    $1"; }
log_err()  { echo -e "${RED}[ERROR]${NC} $1"; }
log_info() { echo -e "${YELLOW}[INFO]${NC}  $1"; }

# -----------------------------------------------------------
# BƯỚC 0: Chờ ES sẵn sàng — tối đa 60 giây
# -----------------------------------------------------------
log_info "Chờ Elasticsearch tại $ES_URL ..."
MAX_WAIT=60; COUNT=0
until curl -s "$ES_URL/_cluster/health" | grep -qE '"status":"(green|yellow)"'; do
  [ $COUNT -ge $MAX_WAIT ] && log_err "ES không phản hồi sau ${MAX_WAIT}s." && exit 1
  sleep 2; COUNT=$((COUNT+2)); echo -n "."
done
echo ""; log_ok "Elasticsearch sẵn sàng."

# -----------------------------------------------------------
# BƯỚC 1: PUT ILM Policy
# -----------------------------------------------------------
# log_info "Đang PUT ILM policy..."
# HTTP=$(curl -s -o /dev/null -w "%{http_code}" \
#   -X PUT "$ES_URL/_ilm/policy/logs-ilm-policy" \
#   -H "Content-Type: application/json" -d @"$ILM_FILE")
# [ "$HTTP" -eq 200 ] \
#   && log_ok "ILM policy 'logs-ilm-policy' OK (HTTP $HTTP)" \
#   || { log_err "Thất bại HTTP $HTTP"; exit 1; }

# -----------------------------------------------------------
# BƯỚC 2: PUT Index Template
# -----------------------------------------------------------
log_info "Đang PUT index template..."
HTTP=$(curl -s -o /dev/null -w "%{http_code}" \
  -X PUT "$ES_URL/_index_template/sys-logs-template" \
  -H "Content-Type: application/json" -d @"$TEMPLATE_FILE")
[ "$HTTP" -eq 200 ] \
  && log_ok "Index template 'sys-logs-template' OK (HTTP $HTTP)" \
  || { log_err "Thất bại HTTP $HTTP"; exit 1; }

# -----------------------------------------------------------
# BƯỚC 3: Xác nhận
# -----------------------------------------------------------
# echo ""
# log_info "=== ILM Policy ==="
# curl -s "$ES_URL/_ilm/policy/logs-ilm-policy" | python3 -m json.tool 2>/dev/null \
#   || curl -s "$ES_URL/_ilm/policy/logs-ilm-policy"

echo ""
log_info "=== Index Template ==="
curl -s "$ES_URL/_index_template/sys-logs-template" | python3 -m json.tool 2>/dev/null \
  || curl -s "$ES_URL/_index_template/sys-logs-template"

echo ""
log_ok "Khởi tạo hoàn tất! Bây giờ có thể start Logstash và Filebeat."