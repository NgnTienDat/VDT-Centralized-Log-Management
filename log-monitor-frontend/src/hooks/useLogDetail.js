import { useQuery } from "@tanstack/react-query";
import { fetchLogById } from "../api/logApi";

/**
 * Hook fetch chi tiết 1 log theo docId.
 *
 * List log (useLogQuery) chỉ trả các field tóm tắt — không có stackTrace,
 * appName, hostName, logger. Khi user click vào 1 row để xem full detail,
 * dùng hook này để fetch riêng document đó.
 *
 * @param {string|null|undefined} docId - docId của log cần xem detail.
 *   Truyền null/undefined khi chưa có row nào được chọn → query sẽ không
 *   chạy (enabled: false), tránh gọi API thừa.
 *
 * @returns {{
 *   detail: object|null,      // full log detail đã map field, hoặc null nếu chưa có
 *   isLoading: boolean,       // true trong lần fetch đầu cho docId hiện tại
 *   isFetching: boolean,
 *   isError: boolean,
 *   error: Error|null,
 * }}
 */
export function useLogDetail(docId) {
    const query = useQuery({
        queryKey: ["log-detail", docId],
        queryFn: () => fetchLogById(docId, "doc_id"),

        // Chỉ fetch khi có docId — tránh gọi API khi chưa chọn log nào
        enabled: !!docId,

        // Detail của 1 log không đổi sau khi đã ghi — cache dài, khỏi refetch lại
        staleTime: 5 * 60_000,
    });

    const item = query.data;

    const detail = item
        ? {
            id: item.id,
            docId: item.docId,
            timestamp: item.eventTimestamp || item.timestamp,
            sentTimestamp: item.sentTimestamp,
            receivedTimestamp: item.receivedTimestamp,
            deliveryLatencyMs: item.deliveryLatencyMs,
            level: (item.logLevel || item.level)?.toUpperCase() || "INFO",
            env: (item.environment || item.env)?.toUpperCase() || "DEV",
            service: item.serviceName || item.service,
            message: item.logMessage || item.message,
            thread: item.thread,
            traceId: item.traceId,
            appName: item.appName,
            hostName: item.hostName,
            logger: item.logger,
            stackTrace: item.stackTrace,
        }
        : null;

    return {
        detail,
        isLoading: query.isLoading,
        isFetching: query.isFetching,
        isError: query.isError,
        error: query.error,
    };
}