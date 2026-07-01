import { formatTs } from "../../utils/constants.js";

// ── Level config ─────────────────────────────────────────────────────────────
const LEVEL = {
    ERROR: {
        bar:   "bg-[#A32D2D]",
        badge: "bg-[#FCEBEB] text-[#A32D2D] dark:bg-[#501313] dark:text-[#F7C1C1]",
        badgeDark: "bg-[#501313] text-[#F7C1C1]",
        badgeLight: "bg-[#FCEBEB] text-[#A32D2D]",
        msg:   { dark: "text-red-300",   light: "text-red-700" },
        rowBg: { dark: "bg-red-500/5",   light: "bg-red-50/60" },
    },
    WARN: {
        bar:   "bg-[#BA7517]",
        badgeDark: "bg-[#412402] text-[#FAC775]",
        badgeLight: "bg-[#FAEEDA] text-[#854F0B]",
        msg:   { dark: "text-amber-200", light: "text-amber-800" },
        rowBg: { dark: "bg-amber-500/4", light: "bg-amber-50/60" },
    },
    INFO: {
        bar:   "bg-[#185FA5]",
        badgeDark: "bg-[#042C53] text-[#B5D4F4]",
        badgeLight: "bg-[#E6F1FB] text-[#185FA5]",
        msg:   { dark: "text-slate-300", light: "text-slate-700" },
        rowBg: { dark: "",               light: "" },
    },
    DEBUG: {
        bar:   "bg-slate-500",
        badgeDark: "bg-[#2C2C2A] text-[#B4B2A9]",
        badgeLight: "bg-[#F1EFE8] text-[#5F5E5A]",
        msg:   { dark: "text-slate-400", light: "text-slate-600" },
        rowBg: { dark: "",               light: "" },
    },
};

function getLvl(level) {
    return LEVEL[level?.toUpperCase()] ?? LEVEL.DEBUG;
}

/**
 * LogRowV2 — card-style log row ported from log.html.
 *
 * Layout (vertical card):
 *   [left bar] | header (ts · level · service · app · env · traceId)
 *              | message
 *              | meta row  (logger · thread · host · duration)
 *              | stackTrace (collapsible pre block, ERROR only)
 *
 * Props: same as LogRow — { log, onClick, selected, isDark }
 */
export default function LogRowV2({ log, onClick, selected, isDark }) {
    const lvl = getLvl(log.level);
    const theme = isDark ? "dark" : "light";

    // ── Row background ──────────────────────────────────────────────────────
    const rowBg = selected
        ? isDark ? "bg-white/6" : "bg-indigo-50"
        : lvl.rowBg[theme];

    const borderSel = selected
        ? isDark ? "border-white/20" : "border-indigo-200"
        : isDark ? "border-white/6"  : "border-slate-200";

    // ── Badge helpers ───────────────────────────────────────────────────────
    const neutralBadge = isDark
        ? "bg-white/8 text-slate-400"
        : "bg-slate-100 text-slate-500";

    const traceBadge = isDark
        ? "bg-[#26215C] text-[#CECBF6]"
        : "bg-[#EEEDFE] text-[#534AB7]";

    return (
        <div
            onClick={() => onClick(log)}
            className={`relative flex gap-3 px-4 py-3 border-b cursor-pointer transition-colors duration-100 ${rowBg} ${borderSel}`}
            style={{ borderLeftColor: undefined }}
        >
            {/* ── Left severity bar ── */}
            <div className={`absolute left-0 top-0 bottom-0 w-0.75 rounded-l-sm ${lvl.bar}`} />

            {/* ── Content ── */}
            <div className="flex-1 min-w-0 pl-1">

                {/* Header row */}
                <div className="flex flex-wrap items-center gap-1.5 mb-1.5">
                    {/* Timestamp */}
                    <span className={`font-mono text-[11px] whitespace-nowrap ${isDark ? "text-slate-500" : "text-slate-400"}`}>
                        {formatTs(log.timestamp)}
                    </span>

                    {/* Level badge */}
                    <span className={`font-mono text-[10px] font-semibold px-1.5 py-px rounded-full uppercase tracking-wide ${lvl[`badge${isDark ? "Dark" : "Light"}`]}`}>
                        {log.level}
                    </span>

                    {/* Service */}
                    {log.service && (
                        <span className={`font-mono text-[10px] px-1.5 py-px rounded-full ${neutralBadge}`}>
                            {log.service}
                        </span>
                    )}

                    {/* App — only if different from service */}
                    {log.appName && log.appName !== log.service && (
                        <span className={`font-mono text-[10px] px-1.5 py-px rounded-full ${neutralBadge}`}>
                            {log.appName}
                        </span>
                    )}

                    {/* Environment */}
                    {log.env && (
                        <span className={`font-mono text-[10px] px-1.5 py-px rounded-full ${neutralBadge}`}>
                            {log.env}
                        </span>
                    )}

                    {/* Trace ID — truncated to 8 chars */}
                    {log.traceId && (
                        <span
                            className={`font-mono text-[10px] px-1.5 py-px rounded-full ${traceBadge}`}
                            title={log.traceId}
                        >
                            {log.traceId.slice(0, 8)}…
                        </span>
                    )}
                </div>

                {/* Message */}
                <div className={`font-mono text-xs leading-relaxed wrap-break-word ${lvl.msg[theme]}`}>
                    {log.message}
                </div>

                {/* Meta row: logger · thread · host · duration */}
                {(log.logger || log.thread || log.hostName || log.durationMs != null) && (
                    <div className={`flex flex-wrap gap-3 mt-1.5 font-mono text-[11px] ${isDark ? "text-slate-600" : "text-slate-400"}`}>
                        {log.logger    && <span>logger: {log.logger}</span>}
                        {log.thread    && <span>thread: {log.thread}</span>}
                        {log.hostName  && <span>host: {log.hostName}</span>}
                        {log.durationMs != null && <span>duration: {log.durationMs}ms</span>}
                    </div>
                )}

                {/* Stack trace */}
                {log.stackTrace && (
                    <pre
                        className={`mt-2 px-3 py-2 rounded-lg font-mono text-[11px] whitespace-pre-wrap break-all max-h-28 overflow-y-auto leading-relaxed
                            ${isDark ? "bg-black/40 text-red-300" : "bg-red-50 text-red-700"}`}
                    >
                        {log.stackTrace}
                    </pre>
                )}
            </div>
        </div>
    );
}
