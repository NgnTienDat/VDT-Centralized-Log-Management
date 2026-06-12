import { SERVICES } from "../../utils/constants.js";

export default function ServiceFilter({ value, onChange, isDark }) {
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
                {SERVICES.map((s) => (
                    <option key={s} value={s}>
                        {s}
                    </option>
                ))}
            </select>
        </div>
    );
}