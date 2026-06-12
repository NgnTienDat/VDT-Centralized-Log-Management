import { ENVIRONMENTS, envConfig } from "../../utils/constants.js";

export default function AppFooter({ logs, isDark }) {
    return (
        <div className="mt-5 flex justify-between items-center">
            <div className={`text-[11px] font-mono ${isDark ? "text-slate-800" : "text-slate-400"}`}>
                LogRadar v1.0 · Elasticsearch + Logstash + Spring Boot ·{" "}
                {ENVIRONMENTS.slice(1).join(" / ")}
            </div>
            <div className="flex gap-4">
                {Object.entries(envConfig).map(([env, cfg]) => (
                    <span
                        key={env}
                        className="flex items-center gap-1.5 text-[11px] font-mono"
                        style={{ color: cfg.hex }}
                    >
                        <span
                            className="w-1.5 h-1.5 rounded-full inline-block"
                            style={{ background: cfg.hex }}
                        />
                        {env}: {logs.filter((l) => l.env === env).length}
                    </span>
                ))}
            </div>
        </div>
    );
}