import { create } from "zustand";

/**
 * Zustand store cho UI filter state.
 *
 * Đây là "nguồn sự thật" cho tất cả filter đang active.
 * useLogQuery đọc store này để build queryKey và params.
 * Khi bất kỳ setter nào được gọi → queryKey thay đổi → TanStack tự refetch.
 *
 * Giá trị mặc định = undefined (không filter) thay vì "ALL"
 * để buildParams trong logApi.js có thể bỏ qua chúng dễ dàng.
 */
export const useFilterStore = create((set) => ({
    environment: undefined,  // "DEV" | "STAGING" | "TEST" | "PROD" | undefined
    logLevel: undefined,  // "INFO" | "WARN" | "ERROR" | "DEBUG" | undefined
    serviceName: undefined,  // "auth-service" | ... | undefined
    appName: undefined,  // "logs-app" | ... | undefined
    q: undefined,  // keyword full-text search | undefined


    // Nhận "ALL" hoặc undefined → convert về undefined để buildParams bỏ qua
    setEnvironment: (value) => set({ environment: value === "ALL" ? undefined : value }),

    setLogLevel: (value) => set({ logLevel: value === "ALL" ? undefined : value }),

    setServiceName: (value) => set({ serviceName: value === "ALL" ? undefined : value }),

    setAppName: (value) => set({ appName: value === "ALL" ? undefined : value }),

    setQ: (value) => set({ q: value?.trim() || undefined }),

    // Reset tất cả về mặc định — hữu ích khi user bấm "Clear all filters"
    resetFilters: () => set({
        environment: undefined,
        logLevel: undefined,
        serviceName: undefined,
        appName: undefined,
        q: undefined
    }),
}));