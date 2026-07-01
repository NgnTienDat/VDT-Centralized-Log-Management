import axiosClient from "./axiosClient";

/**
 * Pure HTTP functions cho Alert Rules — không có state, không có hook.
 * Chỉ nhận params, gọi API, trả về data.
 *
 * Tầng này dễ mock trong unit test:
 *   vi.mock("@/api/alertApi", () => ({ createNewRule: vi.fn() }))
 */

// ── Alert Rule API ───────────────────────────────────────────────────────────

/**
 * Tạo mới một Alert Rule.
 *
 * @param {Object} ruleConfig - Cấu trúc RuleConfig phía backend
 * @param {string} ruleConfig.name
 * @param {number} ruleConfig.intervalMinutes
 * @param {boolean} [ruleConfig.isActive]
 * @param {string} ruleConfig.triggerStepId   - ID của step (A, B, C...) quyết định Firing/OK
 * @param {Array}  ruleConfig.pipelineSteps   - Danh sách step (FETCH_ES_DATA, MATH, EVALUATE_THRESHOLD)
 * @param {number} [ruleConfig.repeatIntervalMinutes]
 * @param {Object} [ruleConfig.notificationTemplate] - { title, message }
 *
 * @returns {Promise<RuleConfig>} Rule đã được lưu (kèm ruleId do backend sinh ra)
 *
 * Lưu ý: index/timeField không xuất hiện trong ruleConfig — theo thiết kế UI,
 * 2 field này bị ẩn khỏi người dùng và phải được điền sẵn (hard-coded hoặc mặc định)
 * trước khi gọi hàm này, không phải trách nhiệm của tầng API.
 */
export async function createNewRule(ruleConfig) {
    // axiosClient interceptor đã unwrap ApiResponse → nhận thẳng phần `data`
    // Tức là response ở đây chính là RuleConfig đã lưu
    const response = await axiosClient.post("/api/v1/alerts/rules", ruleConfig);
    return response.data;
}


/**
 * Lấy danh sách toàn bộ Alert Rules.
 * @returns {Promise<RuleConfig[]>}
 */
export async function getAllRules() {
    const response = await axiosClient.get("/api/v1/alerts/rules");
    return response.data;
}

/**
 * Lấy chi tiết 1 Alert Rule theo ID.
 * @param {string} ruleId
 * @returns {Promise<RuleConfig>}
 */
export async function getRuleById(ruleId) {
    const response = await axiosClient.get(`/api/v1/alerts/rules/${ruleId}`);
    return response.data;
}

/**
 * Cập nhật 1 phần Alert Rule (PATCH). Field nào không truyền = giữ nguyên,
 * khớp với UpdateRuleRequest backend (toàn bộ field đều nullable).
 * @param {string} ruleId
 * @param {Object} updateRuleRequest - { name?, intervalMinutes?, isActive?, repeatIntervalMinutes?, triggerStepId?, pipelineSteps?, notificationTemplate? }
 * @returns {Promise<RuleConfig>}
 */
export async function updateRule(ruleId, updateRuleRequest) {
    const response = await axiosClient.patch(`/api/v1/alerts/rules/${ruleId}`, updateRuleRequest);
    return response.data;
}

/**
 * Xoá 1 Alert Rule.
 * @param {string} ruleId
 */
export async function deleteRule(ruleId) {
    const response = await axiosClient.delete(`/api/v1/alerts/rules/${ruleId}`);
    return response.data;
}

/**
 * Lấy lịch sử thông báo cảnh báo lưu trong Elasticsearch theo ruleId.
 * Đường dẫn: /api/v1/alerts/rules/{ruleId}/notifications
 * @param {string} ruleId
 * @returns {Promise<Array>} Danh sách các thông báo cảnh báo
 */
export async function getNotificationsByRule(ruleId) {
    const response = await axiosClient.get(`/api/v1/alerts/rules/${ruleId}/notifications`);
    return response.data || [];
}