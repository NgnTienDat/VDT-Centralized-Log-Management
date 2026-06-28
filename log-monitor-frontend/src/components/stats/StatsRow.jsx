import StatCard from "./StatCard.jsx";
import MiniChart from "./MiniChart.jsx";

export default function StatsRow({ stats, errorHistory, isDark }) {
    const { total, error, warn } = stats;

    const cardClass = isDark
        ? "bg-white/3 border-white/6"
        : "bg-white border-slate-200";

    return (
        <div className="grid grid-cols-3 gap-3 mb-5">
            <StatCard
                label="Total Logs"
                value={total}
                color={isDark ? "#94a3b8" : "#374151"}
                sublabel="last session"
                isDark={isDark}
            />
            <StatCard
                label="Errors"
                value={error}
                color="#ff4d4d"
                sublabel={`${((error / total) * 100).toFixed(1)}% of total`}
                isDark={isDark}
            />
            <StatCard
                label="Warnings"
                value={warn}
                color="#f59e0b"
                sublabel={`${((warn / total) * 100).toFixed(1)}% of total`}
                isDark={isDark}
            />
            {/* <div className={`border rounded-xl px-4 py-3.5 transition-colors duration-200 ${cardClass}`}>
                <div className="text-[11px] text-slate-500 font-mono tracking-widest uppercase mb-1.5">
                    Error Rate (20m)
                </div>
                <MiniChart data={errorHistory} />
            </div> */}
        </div>
    );
}