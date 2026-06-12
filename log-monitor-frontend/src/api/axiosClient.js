import axios from "axios";

const axiosClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8082",
    timeout: 10_000,
    headers: {
        "Content-Type": "application/json",
    },
});


axiosClient.interceptors.response.use(
    (response) => {
        return response.data;
    },
    (error) => {
        // Chuẩn hóa error object trước khi TanStack Query nhận
        const status = error.response?.status;
        const message = error.response?.data?.message ?? error.message ?? "Unknown error";

        // Log ra console để dev debug dễ hơn — chỉ ở môi trường dev
        if (import.meta.env.DEV) {
            console.error(`[API Error] ${status} — ${message}`, error.config?.url);
        }

        // Trả về error đã được chuẩn hóa để TanStack Query xử lý
        return Promise.reject({ status, message, original: error });
    }
);

export default axiosClient;