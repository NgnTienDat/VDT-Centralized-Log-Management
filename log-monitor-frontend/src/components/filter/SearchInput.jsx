export default function SearchInput({ value, onChange, isDark }) {
    return (
        <div className="relative flex-1 min-w-50">
            <span className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-500 text-sm pointer-events-none">
                ⌕
            </span>
            <input
                value={value}
                onChange={(e) => onChange(e.target.value)}
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