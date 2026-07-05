import axiosClient from "./axiosClient";

/**
 * Pure HTTP functions — không có state, không có hook.
 * Chỉ nhận params, gọi API, trả về data.
 *
 * Tầng này dễ mock trong unit test:
 *   vi.mock("@/api/logApi", () => ({ fetchLogs: vi.fn() }))
 */

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Xây URLSearchParams từ object, tự động bỏ qua các field null/undefined/"ALL".
 *
 * Ví dụ:
 *   buildParams({ env: "DEV", level: "ALL", size: 50 })
 *   → "environment=DEV&size=50"   (level bị bỏ vì là "ALL")
 */
function buildParams(raw) {
    const params = new URLSearchParams();

    Object.entries(raw).forEach(([key, value]) => {
        // Bỏ qua: undefined, null, chuỗi rỗng, và giá trị "ALL" (= không filter)
        if (value === undefined || value === null || value === "" || value === "ALL") return;
        params.append(key, value);
    });

    return params;
}

// ── Log API ───────────────────────────────────────────────────────────────────

/**
 * Fetch danh sách log với cursor-based pagination.
 *
 * @param {Object} filters - Các filter đang active
 * @param {string} [filters.environment]  - "DEV" | "STAGING" | "TEST" | "PROD"
 * @param {string} [filters.appName]      - tên app
 * @param {string} [filters.serviceName]  - tên service
 * @param {string} [filters.logLevel]     - "INFO" | "WARN" | "ERROR" | "DEBUG"
 * @param {string} [filters.q]            - keyword full-text search
 *
 * @param {Object} [cursor] - Cursor từ response trước, undefined cho lần đầu
 * @param {string} [cursor.before]    - nextCursor (ISO timestamp)
 * @param {string} [cursor.beforeId]  - nextCursorId (ES document id, tie-breaking)
 *
 * @param {number} [size=10] - Số log mỗi trang
 *
 * @returns {Promise<CursorPage>}
 *   CursorPage { data: LogEntry[], hasMore: boolean, nextCursor: string, nextCursorId: string }
 *
 * Tại sao cần cả `before` và `beforeId`?
 *   Elasticsearch sort theo @timestamp DESC. Nếu 2 log có cùng timestamp
 *   (xảy ra khi service log nhiều dòng trong cùng 1ms), chỉ dùng timestamp
 *   làm cursor sẽ bị duplicate hoặc bỏ sót log ở page kế tiếp.
 *   beforeId là _id của document cuối — ES dùng nó như tie-breaker để
 *   đảm bảo kết quả deterministic.
 */
export async function fetchLogs(filters = {}, cursor = undefined, size = 50) {
    const params = buildParams({
        environment: filters.environment?.toLocaleLowerCase(),
        appName: filters.appName,
        serviceName: filters.serviceName,
        logLevel: filters.logLevel,
        q: filters.q,
        before: cursor?.before,
        beforeId: cursor?.beforeId,
        size,
    });

    // axiosClient interceptor đã unwrap ApiResponse → nhận thẳng phần `data`
    // Tức là response ở đây là CursorPage { data, hasMore, nextCursor, nextCursorId }
    // console.log("Fetching logs with params:", params.toString());
    const response = await axiosClient.get(`/api/v1/logs?${params}`);
    return response.data;
}


/**
 * Fetch chi tiết 1 log.
 * Dùng khi user click vào LogRow để xem full detail + stackTrace.
 *
 * Backend hỗ trợ 2 cách lookup qua query param `by`:
 *   GET /api/v1/logs/{value}              → tìm theo ES _id        (by mặc định = "id")
 *   GET /api/v1/logs/{value}?by=doc_id    → tìm theo field doc_id
 *
 * List log (fetchLogs) trả về cả `id` (ES _id) và `docId` (UUID nghiệp vụ).
 * UI thường lưu/điều hướng theo `docId`, nên mặc định ở đây dùng "doc_id".
 *
 * @param {string} value - id hoặc docId tùy `by`
 * @param {"id"|"doc_id"} [by="doc_id"]
 * @returns {Promise<LogEntry>} full log detail (bao gồm stackTrace, appName, hostName, logger)
 */
export async function fetchLogById(value, by = "doc_id") {
    const params = buildParams({ by });
    const response = await axiosClient.get(`/api/v1/logs/${value}?${params}`);
    return response.data;
}


/**
 * Fetch danh sách các microservices duy nhất hiện có từ Elasticsearch aggregations.
 * @returns {Promise<string[]>} Mảng các tên service (ví dụ: ["logs-service", "auth-service"])
 */
export async function fetchServices() {
    // axiosClient đã unwrap ApiResponse nên nhận thẳng response.data
    const response = await axiosClient.get("/api/v1/logs/services");
    return response.data;
}

export async function fetchApps() {
    // axiosClient đã unwrap ApiResponse nên nhận thẳng response.data
    const response = await axiosClient.get("/api/v1/logs/applications");
    return response.data;
}