/**
 * Query key factory — định nghĩa tất cả queryKey tập trung ở 1 chỗ.
 *
 * Tại sao cần file này?
 * Nếu queryKey bị hardcode rải rác ở nhiều hooks, khi cần invalidate
 * (VD: sau khi live mode push log mới vào cache) dễ bị typo hoặc mismatch.
 *
 * Dùng:
 *   queryClient.invalidateQueries({ queryKey: QUERY_KEYS.logs.all })
 *   queryClient.setQueryData(QUERY_KEYS.logs.list(filters), updater)
 */
export const QUERY_KEYS = {
    logs: {
        // Invalidate tất cả query liên quan đến logs (mọi filter combination)
        all: ["logs"],

        // Key cụ thể cho 1 bộ filter — dùng trong useLogQuery
        list: (filters) => ["logs", filters],

        // Key cho detail 1 log cụ thể — dùng trong useLogDetail
        detail: (id) => ["logs", "detail", id],
    },

    stats: {
        all: ["stats"],
        summary: ["stats", "summary"],
    },

    alerts: {
        all: ["alerts"],
        active: ["alerts", "active"],
    },
};