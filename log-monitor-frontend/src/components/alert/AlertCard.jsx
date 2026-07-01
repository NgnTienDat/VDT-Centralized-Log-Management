/**
 * AlertCard — renders a single alert item from the /topic/alerts STOMP feed.
 * Severity: CRITICAL | HIGH | MEDIUM | LOW
 */

const SEV_CONFIG = {
    CRITICAL: {
        icon: "🔴",
        bar: "bg-red-500",
        iconBg: "bg-red-500/10",
        badge: "bg-red-500/10 text-red-400",
        border: "border-red-500/20",
    },
    HIGH: {
        icon: "🟠",
        bar: "bg-orange-500",
        iconBg: "bg-orange-500/10",
        badge: "bg-orange-500/10 text-orange-400",
        border: "border-orange-500/20",
    },
    MEDIUM: {
        icon: "🟡",
        bar: "bg-yellow-500",
        iconBg: "bg-yellow-500/10",
        badge: "bg-yellow-500/10 text-yellow-400",
        border: "border-yellow-500/20",
    },
    LOW: {
        icon: "🟢",
        bar: "bg-green-500",
        iconBg: "bg-green-500/10",
        badge: "bg-green-500/10 text-green-400",
        border: "border-green-500/20",
    },
};

function fmt(iso) {
    if (!iso) return "—";
    try {
        return new Date(iso).toLocaleString("vi-VN");
    } catch {
        return iso;
    }
}

export default function AlertCard({ alert, isDark }) {
    const sev = (alert.severity || "MEDIUM").toUpperCase();
    const cfg = SEV_CONFIG[sev] ?? SEV_CONFIG.MEDIUM;

    return (
        <div
            className={`relative flex gap-3 rounded-xl border p-4 overflow-hidden
                ${isDark
                    ? `bg-[#151821] ${cfg.border}`
                    : `bg-white ${cfg.border}`
                }
                animate-[slideIn_0.3s_ease]`}
            style={{ animationName: "slideIn" }}
        >
            {/* Left severity bar */}
            <div className={`absolute left-0 top-0 bottom-0 w-0.5 rounded-l-xl ${cfg.bar}`} />

            {/* Icon */}
            <div className={`shrink-0 w-9 h-9 rounded-xl flex items-center justify-center text-base ${cfg.iconBg}`}>
                {cfg.icon}
            </div>

            {/* Body */}
            <div className="flex-1 min-w-0">
                <div className={`text-sm font-semibold mb-0.5 truncate ${isDark ? "text-slate-200" : "text-slate-800"}`}>
                    {alert.title || alert.ruleId || "Alert"}
                </div>
                <div className={`text-xs mb-2 leading-relaxed ${isDark ? "text-slate-400" : "text-slate-500"}`}>
                    {alert.message || ""}
                </div>
                <div className="flex flex-wrap gap-1.5">
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-md tracking-wide ${cfg.badge}`}>
                        {sev}
                    </span>
                    {alert.ruleId && (
                        <span className={`text-[10px] px-2 py-0.5 rounded-md font-medium ${isDark ? "bg-white/5 text-slate-400" : "bg-slate-100 text-slate-500"}`}>
                            🆔 {alert.ruleId}
                        </span>
                    )}
                    {alert.environment && (
                        <span className={`text-[10px] px-2 py-0.5 rounded-md font-medium ${isDark ? "bg-white/5 text-slate-400" : "bg-slate-100 text-slate-500"}`}>
                            🌍 {alert.environment}
                        </span>
                    )}
                    {alert.application && (
                        <span className={`text-[10px] px-2 py-0.5 rounded-md font-medium ${isDark ? "bg-white/5 text-slate-400" : "bg-slate-100 text-slate-500"}`}>
                            📦 {alert.application}
                        </span>
                    )}
                    {alert.matchedCount != null && (
                        <span className={`text-[10px] px-2 py-0.5 rounded-md font-medium ${isDark ? "bg-white/5 text-slate-400" : "bg-slate-100 text-slate-500"}`}>
                            📈 {alert.matchedCount} logs
                        </span>
                    )}
                </div>
            </div>

            {/* Timestamp */}
            <div className={`shrink-0 text-[10px] mt-0.5 ${isDark ? "text-slate-500" : "text-slate-400"}`}>
                {fmt(alert.triggeredAt)}
            </div>
        </div>
    );
}
