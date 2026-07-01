import { useState, useEffect } from "react";

export default function SearchInput({ value, onChange, isDark }) {
    // 1. Tạo một state local để lưu chữ người dùng đang gõ (giúp UI mượt mà, không bị delay)
    const [localValue, setLocalValue] = useState(value || "");

    // 2. Đồng bộ ngược lại nếu giá trị từ ngoài Store thay đổi (ví dụ khi user bấm nút "Clear All Filters")
    useEffect(() => {
        setLocalValue(value || "");
    }, [value]);

    // 3. Cơ chế Debounce: Đợi user ngừng gõ 400ms rồi mới đẩy giá trị ra Zustand store ngoài
    useEffect(() => {
        const timer = setTimeout(() => {
            onChange(localValue);
        }, 500); // 500 mili-giây là khoảng thời gian lý tưởng cho gõ phím

        // Hàm cleanup này tự động chạy để hủy bỏ timer cũ nếu user tiếp tục nhấn phím tiếp theo
        return () => clearTimeout(timer);
    }, [localValue, onChange]);

    return (
        <div className="relative flex-1 min-w-50">
            <span className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-500 text-sm pointer-events-none">
                ⌕
            </span>
            <input
                value={localValue}
                onChange={(e) => setLocalValue(e.target.value)} // Cập nhật state local ngay lập tức
                placeholder="Search messages, traceId, service..."
                className={[
                    "w-full rounded-md py-1.5 pl-8 pr-3 text-xs font-mono outline-none",
                    "border transition-colors duration-200",
                    isDark
                        ? "bg-white/4 border-white/8 text-slate-200 placeholder-slate-600 focus:border-white/20"
                        : "bg-slate-50 border-slate-300 text-slate-800 placeholder-slate-400 focus:border-slate-400",
                ].join(" ")}
            />
        </div>
    );
}