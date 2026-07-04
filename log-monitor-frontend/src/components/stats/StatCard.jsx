export default function StatCard({ label, value, color, sublabel, isDark }) {
    const cardClass = isDark
        ? "bg-white/3 border-white/6"
        : "bg-white border-slate-200";

    return (
        <div className={`border rounded-xl px-4 py-3.5 min-w-0 transition-colors duration-200 ${cardClass}`}>
            <div className="text-[11px] text-slate-600 font-mono tracking-widest uppercase mb-1.5">
                {label}
            </div>
            <div className="text-3xl font-bold font-mono leading-none" style={{ color }}>
                {value}
            </div>
            {sublabel && (
                <div className="text-[11px] text-slate-700 font-mono mt-1">{sublabel}</div>
            )}
        </div>
    );
}