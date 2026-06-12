export default function MiniChart({ data = [] }) {
    if (!data || data.length === 0) return <div className="h-10" />;
    const max = Math.max(...data, 1);

    return (
        <div className="flex items-end gap-0.75 h-10">
            {data.map((v, i) => (
                <div
                    key={i}
                    className="flex-1 rounded-xs transition-all duration-300"
                    style={{
                        background:
                            v > 0
                                ? `rgba(255,77,77,${0.3 + (v / max) * 0.7})`
                                : "rgba(255,255,255,0.04)",
                        height: `${Math.max((v / max) * 100, 4)}%`,
                    }}
                    title={`${v} errors`}
                />
            ))}
        </div>
    );
}