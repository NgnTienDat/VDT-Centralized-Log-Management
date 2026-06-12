export default function AppHeader({ isDark, onToggleTheme, liveMode, onToggleLive, alertCount }) {
    const headerClass = isDark
        ? "bg-[#0a0f1a] border-white/6"
        : "bg-white border-slate-200 shadow-sm";

    const logoBgClass = isDark
        ? "bg-gradient-to-br from-sky-400/15 to-violet-600/20 border-sky-400/30"
        : "bg-gradient-to-br from-sky-100 to-violet-100 border-violet-200";

    const themeButtonClass = isDark
        ? "bg-white/3 border-white/6 text-slate-500 hover:text-slate-300"
        : "bg-white border-slate-200 text-slate-500 hover:text-slate-700";

    const liveButtonClass = liveMode
        ? "bg-red-500/10 border-red-500/40 text-red-400"
        : isDark
            ? "bg-white/3 border-white/6 text-slate-500"
            : "bg-white border-slate-200 text-slate-500";

    return (
        <div className={`border-b px-6 flex items-center justify-between h-14 transition-colors duration-200 ${headerClass}`}>
            {/* Logo */}
            <div className="flex items-center gap-3.5">
                <div className={`w-8 h-8 rounded-lg border flex items-center justify-center text-base ${logoBgClass}`}>
                    ◈
                </div>
                <div>
                    <div className={`text-sm font-bold tracking-tight font-mono ${isDark ? "text-slate-200" : "text-slate-800"}`}>
                        LogRadar
                    </div>
                    <div className="text-[10px] tracking-widest text-slate-600 font-mono">
                        SYSTEM LOG MONITOR
                    </div>
                </div>
            </div>

            {/* Right controls */}
            <div className="flex items-center gap-2.5">
                {/* Alert badge */}
                {alertCount > 0 && (
                    <div className="flex items-center gap-1.5 bg-red-500/8 border border-red-500/30 rounded-md px-2.5 py-1">
                        <span className="text-xs">🔴</span>
                        <span className="text-[11px] text-red-400 font-bold font-mono">
                            {alertCount} ALERT{alertCount > 1 ? "S" : ""}
                        </span>
                    </div>
                )}

                {/* Live toggle */}
                <button
                    onClick={onToggleLive}
                    className={`flex items-center gap-2 border rounded-md px-3.5 py-1.5 cursor-pointer font-mono text-[11px] font-bold tracking-wider transition-all duration-200 ${liveButtonClass}`}
                >
                    <span
                        className={`w-2 h-2 rounded-full inline-block ${liveMode ? "animate-pulse bg-red-400" : isDark ? "bg-slate-600" : "bg-slate-400"}`}
                    />
                    {liveMode ? "LIVE" : "PAUSED"}
                </button>

                {/* Theme toggle */}
                <button
                    onClick={onToggleTheme}
                    title={isDark ? "Switch to Light mode" : "Switch to Dark mode"}
                    className={`flex items-center gap-2 border rounded-lg px-3 py-1.5 cursor-pointer font-mono text-[11px] tracking-wider transition-all duration-200 ${themeButtonClass}`}
                >
                    <span className="text-base leading-none">{isDark ? "☀️" : "🌙"}</span>
                    <span className={isDark ? "text-slate-600" : "text-slate-400"}>
                        {isDark ? "LIGHT" : "DARK"}
                    </span>
                </button>

                {/* Clock */}
                <div className="text-[11px] text-slate-700 font-mono ml-1">
                    {new Date().toLocaleTimeString("vi-VN")}
                </div>
            </div>
        </div>
    );
}