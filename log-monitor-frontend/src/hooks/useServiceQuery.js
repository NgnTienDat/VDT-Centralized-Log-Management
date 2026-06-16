import { useQuery } from "@tanstack/react-query"; // 🌟 Thêm useQuery
import { fetchServices } from "../api/logApi";          // 🌟 Thêm fetchServices
import { useFilterStore } from "../stores/useFilterStore";

export function useServicesQuery() {
    const query = useQuery({
        queryKey: ["services"],
        queryFn: fetchServices,
        
        // Danh sách service không thay đổi liên tục, cache 5 phút là hợp lý
        staleTime: 5 * 60 * 1000, 
        
        // Giá trị mặc định khi đang fetch lần đầu, tránh crash UI map()
        placeholderData: [], 
    });

    return {
        services: query.data || [],
        isLoading: query.isLoading,
        isError: query.isError,
        error: query.error,
    };
}