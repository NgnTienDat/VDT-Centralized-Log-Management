function formatVnTime(ts) {
    if (!ts) return "—";
    return new Date(ts).toLocaleString("vi-VN", {
        timeZone: "Asia/Ho_Chi_Minh",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
    });
}

/**
 * Thuần presentational — không biết WebSocket, không tự fetch.
 * Nhận notifications[] từ AlertRuleDetail (đã lọc theo ruleId).
 */
export default function AlertNotification({ notifications = [], isDark }) {
    const textMuted = isDark ? "text-slate-400" : "text-slate-500";
    const bgCard = isDark ? "bg-white/3 border-white/8" : "bg-white border-slate-200 shadow-sm";
    const chipCls = isDark ? "bg-white/8 text-slate-300" : "bg-slate-100 text-slate-600";
    const tableHeaderCls = isDark
        ? "bg-white/3 border-white/8 text-slate-500"
        : "bg-slate-50 border-slate-200 text-slate-400";
    const tableRowBorder = isDark ? "border-white/5" : "border-slate-100";
    const borderSubtle = isDark ? "border-white/8" : "border-slate-200";

    if (notifications.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center py-16 gap-2">
                <span className="text-4xl opacity-20">🔔</span>
                <p className={`text-xs font-medium ${textMuted}`}>Chưa có thông báo trong phiên này</p>
                <p className={`text-[10px] ${textMuted}`}>
                    Thông báo sẽ xuất hiện ở đây khi rule chuyển sang trạng thái FIRING
                </p>
            </div>
        );
    }

    return (
        <div className="alert-rule-detail-scope flex flex-col gap-3">
            <p className={`text-[11px] ${textMuted}`}>
                {notifications.length} thông báo · mới nhất ở trên cùng
            </p>

            {notifications.map((notif, idx) => {
                const isFiring = notif.alertState === "FIRING";

                const stateBadgeCls = isFiring
                    ? isDark
                        ? "bg-rose-500/10 text-rose-400 border-rose-500/30"
                        : "bg-rose-50 text-rose-700 border-rose-200"
                    : isDark
                        ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/30"
                        : "bg-emerald-50 text-emerald-700 border-emerald-200";

                const valueColor = isFiring
                    ? isDark ? "text-rose-400" : "text-rose-600"
                    : isDark ? "text-emerald-400" : "text-emerald-600";

                const breachedEntries = notif.breachedGroupValues
                    ? Object.entries(notif.breachedGroupValues)
                    : [];

                return (
                    <div key={idx} className={`rounded-lg border p-3.5 flex flex-col gap-2.5 ${bgCard}`}>
                        {/* Row 1: alertState badge + timestamp */}
                        <div className="flex items-center justify-between gap-2 flex-wrap">
                            {/* <span className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded border ${stateBadgeCls}`}>
                                {notif.alertState}
                            </span> */}
                            <span className={`text-[10px] font-mono ${textMuted}`}>
                                🕐 {formatVnTime(notif.timestamp)}
                            </span>
                        </div>

                        {/* Row 2: title */}
                        <p className="text-sm font-semibold leading-snug">{notif.title}</p>

                        {/* Row 3: message */}
                        {notif.message && (
                            <p className={`text-xs leading-relaxed ${textMuted}`}>{notif.message}</p>
                        )}

                        {/* Row 4: breachedGroupValues */}
                        {breachedEntries.length > 0 && (
                            <div className={`rounded border overflow-hidden ${borderSubtle}`}>
                                <div className={`px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider border-b ${tableHeaderCls}`}>
                                    Giá trị vi phạm theo nhóm
                                </div>
                                {breachedEntries.map(([group, value]) => (
                                    <div
                                        key={group}
                                        className={`flex items-center justify-between px-3 py-2 border-b last:border-b-0 ${tableRowBorder}`}
                                    >
                                        <span className={`font-mono text-[11px] px-1.5 py-0.5 rounded ${chipCls}`}>
                                            {group}
                                        </span>
                                        <span className={`font-mono font-bold text-xs ${valueColor}`}>
                                            {value}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                );
            })}
        </div>
    );
}