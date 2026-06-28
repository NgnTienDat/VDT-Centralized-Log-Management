export default function AppFilter({ value, onChange, apps = [], isDark }) {
    return (
        <div className="flex items-center gap-2">
            <span className="text-[11px] tracking-widest text-slate-600 uppercase">App:</span>
            <select
                value={value}
                onChange={(e) => onChange(e.target.value)}
                className={[
                    "rounded-md px-2.5 py-1.5 font-mono text-[11px] cursor-pointer outline-none",
                    "border transition-colors duration-200",
                    isDark
                        ? "bg-white/4 border-white/8 text-slate-300"
                        : "bg-slate-50 border-slate-300 text-slate-700",
                ].join(" ")}
            >
                {/* 🌟 Luôn có option mặc định để hủy filter app */}
                <option value="ALL">ALL APPS</option>

                {/* Duyệt qua danh sách apps lấy động từ API */}
                {apps.map((app) => (
                    <option key={app} value={app}>
                        {app}
                    </option>
                ))}
            </select>
        </div>
    );
}