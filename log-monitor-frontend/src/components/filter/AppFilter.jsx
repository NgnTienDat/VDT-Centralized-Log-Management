export default function AppFilter({ value, onChange, apps = [], isDark }) {
    // SỬA: Định nghĩa màu nền cố định cho option giống ServiceFilter
    const optionCls = isDark ? "bg-[#0a0f1a] text-slate-300" : "bg-white text-slate-700";

    return (
        <div className="flex items-center gap-2">
            <span className="text-[11px] tracking-widest text-slate-700 uppercase">App:</span>
            <select
                value={value}
                onChange={(e) => onChange?.(e.target.value)} // SỬA: Thêm ?. bảo vệ
                className={[
                    "rounded-md px-2.5 py-1.5 font-mono text-[11px] cursor-pointer outline-none",
                    "border transition-colors duration-200",
                    isDark
                        ? "bg-white/4 border-white/8 text-slate-300"
                        : "bg-slate-50 border-slate-300 text-slate-800",
                ].join(" ")}
            >
                <option value="ALL" className={optionCls}>ALL APPS</option>
                {apps.map((app) => (
                    <option key={app} value={app} className={optionCls}>
                        {app}
                    </option>
                ))}
            </select>
        </div>
    );
}