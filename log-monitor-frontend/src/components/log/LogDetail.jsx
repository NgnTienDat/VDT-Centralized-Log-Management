import { levelConfig } from "../../utils/constants.js";

const FIELDS = [
    ["timestamp", (log) => log.timestamp],
    ["level", (log) => log.level],
    ["environment", (log) => log.env],
    ["service", (log) => log.service],
    ["thread", (log) => log.thread],
    ["traceId", (log) => log.traceId],
    ["message", (log) => log.message],
];

export default function LogDetail({ log, onClose, isDark }) {
    if (!log) return null;

    const lvl = levelConfig[log.level] || levelConfig.INFO;

    const panelClass = isDark
        ? "bg-[#0a0f1a] border-white/6"
        : "bg-white border-slate-200 shadow-sm";

    const rowDivider = isDark ? "border-white/4" : "border-slate-100";

    const valueColor = (key) => {
        if (key === "level") return lvl.hex;
        if (key === "message") return isDark ? "#c8cfe8" : "#1a2035";
        return isDark ? "#94a3b8" : "#374151";
    };

    return (
        <div className={`border rounded-xl p-5 font-mono text-xs mt-4 transition-colors duration-200 ${panelClass}`}>
            {/* Header */}
            <div className="flex justify-between items-center mb-4">
                <span className="font-bold text-[13px]" style={{ color: lvl.hex }}>
                    ▶ Log Detail
                </span>
                <button
                    onClick={onClose}
                    className="bg-transparent border-none text-slate-500 cursor-pointer text-xl leading-none px-1 hover:text-slate-300 transition-colors"
                >
                    ×
                </button>
            </div>

            {/* Fields */}
            {FIELDS.map(([key, getValue]) => (
                <div
                    key={key}
                    className={`grid gap-2 mb-2 border-b pb-2 ${rowDivider}`}
                    style={{ gridTemplateColumns: "110px 1fr" }}
                >
                    <span className="text-slate-600">{key}</span>
                    <span className="break-all" style={{ color: valueColor(key) }}>
                        {getValue(log)}
                    </span>
                </div>
            ))}
        </div>
    );
}