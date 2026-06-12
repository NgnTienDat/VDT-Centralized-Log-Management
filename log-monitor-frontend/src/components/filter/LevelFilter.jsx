import { LOG_LEVELS } from "../../utils/constants.js";

const activeClass = {
    ALL: "bg-white/8 border-white/20 text-slate-200 dark:bg-white/8 dark:border-white/20",
    ERROR: "bg-red-500/12 border-red-500/40 text-red-400",
    WARN: "bg-amber-500/12 border-amber-500/40 text-amber-400",
    INFO: "bg-sky-500/10 border-sky-500/35 text-sky-400",
    DEBUG: "bg-slate-500/10 border-slate-500/30 text-slate-400",
};

const baseClass =
    "border rounded-md px-3 py-1 cursor-pointer font-mono text-[11px] tracking-wider transition-all duration-150";

export default function LevelFilter({ value, onChange, isDark }) {
    const inactiveClass = isDark
        ? "bg-transparent border-white/8 text-slate-500 hover:border-white/22 hover:text-slate-200"
        : "bg-transparent border-slate-300 text-slate-500 hover:border-slate-400 hover:text-slate-700";

    return (
        <div className="flex gap-1.5 flex-wrap">
            {LOG_LEVELS.map((l) => (
                <button
                    key={l}
                    onClick={() => onChange(l)}
                    className={`${baseClass} ${value === l ? activeClass[l] : inactiveClass}`}
                >
                    {l}
                </button>
            ))}
        </div>
    );
}