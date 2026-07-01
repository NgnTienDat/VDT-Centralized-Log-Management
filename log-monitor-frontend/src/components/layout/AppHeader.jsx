export default function AppHeader({ isDark, onToggleTheme }) {
    const headerClass = isDark ? "bg-[#0a0f1a] border-white/6" : "bg-white border-slate-200 shadow-sm";
    const themeButtonClass = isDark
        ? "bg-white/3 border-white/6 text-slate-500 hover:text-slate-300"
        : "bg-white border-slate-200 text-slate-500 hover:text-slate-700";

    return (
        <div className={`flex items-center justify-between px-6 py-3 border-b transition-colors duration-200 ${headerClass}`}>
            <div className="flex items-center gap-2">
                <span className="text-xs font-semibold text-slate-400 dark:text-slate-500 font-mono uppercase tracking-wider">
                    Logs Centralized Monitoring & Alerting
                </span>
            </div>

            {/* Right Controls */}
            <div className="flex items-center gap-4">
                {/* Theme Toggle */}
                <button
                    onClick={onToggleTheme}
                    title={isDark ? "Switch to Light mode" : "Switch to Dark mode"}
                    className={`flex items-center gap-2 border rounded-lg px-3 py-1.5 cursor-pointer font-mono text-[11px] tracking-wider transition-all duration-200 ${themeButtonClass}`}
                >
                    <span className="text-base leading-none">{isDark ? "☀️" : "🌙"}</span>
                    <span className={isDark ? "text-slate-400" : "text-slate-500"}>
                        {isDark ? "LIGHT" : "DARK"}
                    </span>
                </button>


            </div>
        </div>
    );
}