import { useInfiniteQuery } from "@tanstack/react-query";
import { fetchLogs } from "../api/logApi";
import { useFilterStore } from "../stores/useFilterStore";

/**
 * Hook quản lý toàn bộ lifecycle của log list:
 *   - Fetch lần đầu khi mount
 *   - Refetch tự động khi filter thay đổi
 *   - Cursor pagination qua fetchNextPage()
 *   - Cache & deduplication do TanStack Query lo
 *
 * Không cần useState, không cần useEffect để fetch —
 * TanStack Query xử lý hết.
 */
export function useLogQuery(options = {}) {
    const { environment, logLevel, serviceName, appName, q } = useFilterStore();

    const query = useInfiniteQuery({
        /**
         * queryKey là "địa chỉ" của cache entry.
         * Khi bất kỳ filter nào thay đổi → queryKey mới → TanStack tự fetch lại.
         * Không cần gọi refetch() thủ công khi user đổi filter.
         */
        queryKey: ["logs", { environment, logLevel, serviceName, appName, q }],

        /**
         * pageParam = cursor từ page trước.
         * Lần đầu: pageParam = undefined → backend trả 50 log mới nhất.
         * Lần sau: pageParam = { before, beforeId } → backend trả log cũ hơn.
         */
        queryFn: ({ pageParam }) =>
            fetchLogs({ environment, logLevel, serviceName, appName, q }, pageParam),

        /**
         * TanStack gọi hàm này sau mỗi page để biết cursor tiếp theo.
         * Trả về undefined → không còn page tiếp → hasNextPage = false → ẩn nút Load more.
         *
         * lastPage là CursorPage { data, hasMore, nextCursor, nextCursorId }
         * từ response của backend.
         */
        getNextPageParam: (lastPage) => {
            if (!lastPage || !lastPage.hasMore) return undefined;

            // Trả về object cursor — sẽ là pageParam của lần fetch tiếp theo
            return {
                before: lastPage.nextCursor,
                beforeId: lastPage.nextCursorId,
            };
        },

        initialPageParam: undefined,

        /**
         * Không stale ngay lập tức vì có live mode bù vào log mới.
         * 30s là đủ để tránh refetch không cần thiết khi user focus lại tab.
         */
        staleTime: 30_000,
        ...options,
    });

    /**
     * Flatten tất cả pages thành 1 array phẳng để render.
     * pages[0] = 10 log mới nhất
     * pages[1] = 10 log tiếp theo (cũ hơn)
     * ...
     * logs = [...pages[0].data, ...pages[1].data, ...]
     */
    const rawLogs = query.data?.pages.flatMap((page) => page?.data || []) ?? [];

    const mappedLogs = rawLogs.map((item) => ({
        id: item.id,
        docId: item.docId,
        timestamp: item.eventTimestamp || item.timestamp,
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
    }));

    return {
        logs: mappedLogs,                              // array phẳng để render
        isLoading: query.isLoading,  // true chỉ lần fetch đầu tiên
        isFetching: query.isFetching,                  // true khi đang fetch hoặc refetch
        isError: query.isError,
        error: query.error,
        isFetchingNextPage: query.isFetchingNextPage, // true khi đang load more
        hasNextPage: query.hasNextPage,            // false → ẩn nút Load more
        fetchNextPage: query.fetchNextPage,          // gọi khi user bấm Load more
        refetch: query.refetch,                // gọi thủ công nếu cần
    };
}