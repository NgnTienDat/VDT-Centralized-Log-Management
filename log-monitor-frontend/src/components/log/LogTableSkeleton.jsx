export default function LogTableSkeleton({ isDark }) {
    const shimmer = isDark ? "bg-white/5 animate-pulse" : "bg-slate-200 animate-pulse";

    return (
        <div>
            {Array.from({ length: 8 }).map((_, i) => (
                <div
                    key={i}
                    className={`grid gap-x-3 px-3.5 py-2 border-b ${isDark ? "border-white/4" : "border-slate-100"}`}
                    style={{ gridTemplateColumns: "90px 62px 88px 140px 1fr" }}
                >
                    <div className={`h-3 rounded ${shimmer}`} style={{ opacity: 1 - i * 0.1 }} />
                    <div className={`h-3 w-10 rounded ${shimmer}`} style={{ opacity: 1 - i * 0.1 }} />
                    <div className={`h-3 w-12 rounded ${shimmer}`} style={{ opacity: 1 - i * 0.1 }} />
                    <div className={`h-3 w-24 rounded ${shimmer}`} style={{ opacity: 1 - i * 0.1 }} />
                    <div className={`h-3 rounded ${shimmer}`} style={{ opacity: 1 - i * 0.1 }} />
                </div>
            ))}
        </div>
    );
}