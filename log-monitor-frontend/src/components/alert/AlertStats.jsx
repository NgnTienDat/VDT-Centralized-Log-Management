/**
 * AlertStats — compact sidebar showing alert counts by severity.
 */

const ROWS = [
    { key: "total",    label: "Tổng",     color: "text-violet-400" },
    { key: "CRITICAL", label: "Critical", color: "text-red-400" },
    { key: "HIGH",     label: "High",     color: "text-orange-400" },
    { key: "MEDIUM",   label: "Medium",   color: "text-yellow-400" },
    { key: "LOW",      label: "Low",      color: "text-green-400" },
];

export default function AlertStats({ stats, isDark }) {
    return (
        <div className="flex flex-col gap-1.5">
            <div className={`text-[10px] font-bold uppercase tracking-widest mb-1 ${isDark ? "text-slate-500" : "text-slate-400"}`}>
                📊 Thống kê
            </div>
            {ROWS.map(({ key, label, color }) => (
                <div
                    key={key}
                    className={`flex justify-between items-center rounded-lg px-3 py-2 border
                        ${isDark ? "bg-[#151821] border-white/6" : "bg-white border-slate-200"}`}
                >
                    <span className={`text-xs ${isDark ? "text-slate-400" : "text-slate-500"}`}>{label}</span>
                    <span className={`text-base font-bold tabular-nums ${color}`}>
                        {stats[key] ?? 0}
                    </span>
                </div>
            ))}
        </div>
    );
}
