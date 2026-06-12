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
export async function fetchLogs(filters = {}, cursor = undefined, size = 10) {
    const params = buildParams({
        environment: filters.environment,
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
    const response = await axiosClient.get(`/api/v1/logs?${params}`);
    // console.log("Fetched logs:", response.data);
    return response.data; // response.data là CursorPage
}

/**
 * Fetch chi tiết 1 log theo id.
 * Dùng khi user click vào LogRow để xem full detail + stackTrace.
 *
 * @param {string} id - ES document _id
 * @returns {Promise<LogEntry>}
 */
export async function fetchLogById(id) {
    const response = await axiosClient.get(`/api/v1/logs/${id}`);
    return response.data;
}

