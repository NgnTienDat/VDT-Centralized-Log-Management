import StatCard from "./StatCard.jsx";
import MiniChart from "./MiniChart.jsx";

export default function StatsRow({ stats, errorHistory, isDark }) {
    // Đảm bảo bóc tách an toàn tránh lỗi crash undefined
    const { total = 0, error = 0, warn = 0 } = stats || {};

    const cardClass = isDark
        ? "bg-white/3 border-white/6"
        : "bg-white border-slate-200 shadow-xs";

    return (
        // Chuyển grid-cols-3 thành phân bổ 4 cột đáp ứng tốt trên màn hình lớn
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-5">
            <StatCard
                label="Total Logs"
                value={total}
                color={isDark ? "#94a3b8" : "#374151"}
                sublabel="in current session"
                isDark={isDark}
            />
            <StatCard
                label="Errors"
                value={error}
                color="#ff4d4d"
                sublabel={total > 0 ? `${((error / total) * 100).toFixed(1)}% of total` : "0.0% of total"}
                isDark={isDark}
            />
            <StatCard
                label="Warnings"
                value={warn}
                color="#f59e0b"
                sublabel={total > 0 ? `${((warn / total) * 100).toFixed(1)}% of total` : "0.0% of total"}
                isDark={isDark}
            />

            {/* FIX 2: Mở khóa block MiniChart và cấu trúc lại style gọn gàng */}
            <div className={`border rounded-xl px-4 py-3.5 transition-colors duration-200 flex flex-col justify-between ${cardClass}`}>
                <div className="text-[11px] text-slate-600 font-mono tracking-widest uppercase mb-2">
                    Error Rate Trend
                </div>

                <div className="flex-1 flex flex-col justify-end">
                    <MiniChart data={errorHistory} />
                </div>

                <div className="text-[10px] text-slate-600 font-mono mt-2 flex justify-between items-center border-t border-slate-100 dark:border-white/5 pt-1.5">
                    <span>Timeline intervals</span>
                    <span className="text-rose-500 font-semibold animate-pulse flex items-center gap-0.5">
                        ● LIVE
                    </span>
                </div>
            </div>
        </div>
    );
}