import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import App from "./App.jsx";
import "./index.css";

/**
 * QueryClient config cho log monitor dashboard.
 *
 * retry: 1 — thử lại 1 lần nếu request fail.
 *   Mặc định TanStack retry 3 lần — quá nhiều cho internal tool,
 *   user thấy spinner lâu mà không biết có lỗi.
 *
 * staleTime: 0 — data coi là stale ngay sau khi fetch xong.
 *   Log data thay đổi liên tục, không cache lâu ở đây.
 *   Từng query có thể override nếu cần (VD: stats dùng staleTime 30s).
 *
 * refetchOnWindowFocus: false — không refetch khi user alt-tab rồi quay lại.
 *   Live mode đã lo việc push data mới → refetch thêm chỉ gây flash UI.
 */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 0,
      refetchOnWindowFocus: false,
    },
  },
});

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
      {/* DevTools chỉ hiện ở môi trường dev — tự ẩn khi build production */}
      {/* <ReactQueryDevtools initialIsOpen={true} /> */}
    </QueryClientProvider>
  </StrictMode>
);