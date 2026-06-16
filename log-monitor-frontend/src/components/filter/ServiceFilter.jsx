export default function ServiceFilter({ value, onChange, services = [], isDark }) {
    return (
        <div className="flex items-center gap-2">
            <span className="text-[11px] tracking-widest text-slate-600 uppercase">Service:</span>
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
                {/* 🌟 Luôn có option mặc định để hủy filter service */}
                <option value="ALL">ALL SERVICES</option>

                {/* Duyệt qua danh sách services lấy động từ API */}
                {services.map((s) => (
                    <option key={s} value={s}>
                        {s}
                    </option>
                ))}
            </select>
        </div>
    );
}