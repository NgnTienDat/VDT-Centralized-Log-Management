import { ENVIRONMENTS, envConfig } from "../../utils/constants.js";

const baseClass =
    "border rounded-md px-3 py-1 cursor-pointer font-mono text-[11px] tracking-wider transition-all duration-150";

export default function EnvFilter({ value, onChange, isDark }) {
    const inactiveClass = isDark
        ? "bg-transparent border-white/8 text-slate-500 hover:border-white/22 hover:text-slate-200"
        : "bg-transparent border-slate-300 text-slate-500 hover:border-slate-400 hover:text-slate-700";

    const allActiveClass = isDark
        ? "bg-white/8 border-white/20 text-slate-200"
        : "bg-slate-100 border-slate-400 text-slate-700";

    return (
        <div className="flex items-center gap-2 flex-wrap">
            <span className="text-[11px] tracking-widest text-slate-600 uppercase">Env:</span>
            <div className="flex gap-1.5 flex-wrap">
                {ENVIRONMENTS.map((e) => {
                    const isActive = value === e;
                    const cfg = envConfig[e];

                    let activeStyle = {};
                    let activeClass = "";

                    if (isActive) {
                        if (e === "ALL") {
                            activeClass = allActiveClass;
                        } else {
                            activeStyle = {
                                color: cfg.hex,
                                background: `${cfg.hex}1a`,
                                borderColor: `${cfg.hex}66`,
                            };
                        }
                    }

                    return (
                        <button
                            key={e}
                            onClick={() => onChange(e)}
                            style={activeStyle}
                            className={`${baseClass} ${isActive ? activeClass : inactiveClass}`}
                        >
                            {e}
                        </button>
                    );
                })}
            </div>
        </div>
    );
}