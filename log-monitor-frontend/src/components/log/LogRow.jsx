import { levelConfig, envConfig, formatTs } from "../../utils/constants.js";

export default function LogRow({ log, onClick, selected, isDark }) {
    const lvl = levelConfig[log.level] || levelConfig.INFO;
    const env = envConfig[log.env];

    const msgColor =
        log.level === "ERROR"
            ? isDark ? "text-red-300" : "text-red-700"
            : log.level === "WARN"
                ? isDark ? "text-amber-200" : "text-amber-800"
                : isDark ? "text-slate-300" : "text-slate-700";

    const rowBase =
        "grid gap-x-3 items-start px-3.5 py-1.5 cursor-pointer border-b transition-colors duration-100 border-l-[3px]";

    const rowBg = selected
        ? isDark ? "bg-white/5 border-l-current" : "bg-indigo-50 border-l-current"
        : log.level === "ERROR"
            ? "bg-red-500/4"
            : "bg-transparent hover:bg-white/3";

    return (
        <div
            onClick={() => onClick(log)}
            className={`${rowBase} ${rowBg} ${isDark ? "border-b-white/4" : "border-b-slate-100"}`}
            style={{
                gridTemplateColumns: "148px 62px 88px 140px 1fr",
                borderLeftColor: selected ? lvl.hex : "transparent",
            }}
        >
            {/* Timestamp */}
            <span className="font-mono text-[11px] text-slate-600 whitespace-nowrap">
                {formatTs(log.timestamp)}
            </span>

            {/* Level badge */}
            <span
                className={`font-mono text-[10px] font-bold border rounded px-1.5 py-px text-center tracking-wider whitespace-nowrap ${lvl.color} ${lvl.bg} ${lvl.border}`}
                style={{ borderColor: undefined, borderWidth: "1px", borderStyle: "solid" }}
            >
                {/* Use hex colors directly for the badge border since Tailwind dynamic classes don't work */}
                <span
                    className={`font-mono text-[10px] font-bold`}
                    style={{ color: lvl.hex }}
                >
                    {log.level}
                </span>
            </span>

            {/* Env badge */}
            <span
                className="font-mono text-[10px] rounded px-1.5 py-px whitespace-nowrap"
                style={{ color: env?.hex, background: `${env?.hex}1a` }}
            >
                {log.env}
            </span>

            {/* Service */}
            <span className="font-mono text-[11px] text-slate-600 whitespace-nowrap overflow-hidden text-ellipsis">
                {log.service}
            </span>

            {/* Message */}
            <span className={`font-mono text-xs leading-relaxed wrap-break-word ${msgColor}`}>
                {log.message}
            </span>
        </div>
    );
}