import { useQuery } from "@tanstack/react-query"; // 🌟 Thêm useQuery
import { fetchApps } from "../api/logApi";          // 🌟 Thêm fetchServices
import { useFilterStore } from "../stores/useFilterStore";

export function useAppsQuery() {
    const query = useQuery({
        queryKey: ["apps"],
        queryFn: fetchApps,
        
        // Danh sách apps không thay đổi liên tục, cache 5 phút là hợp lý
        staleTime: 5 * 60 * 1000, 
        
        // Giá trị mặc định khi đang fetch lần đầu, tránh crash UI map()
        placeholderData: [], 
    });

    return {
        apps: query.data || [],
        isLoading: query.isLoading,
        isError: query.isError,
        error: query.error,
    };
}