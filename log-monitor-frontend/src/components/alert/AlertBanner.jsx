export default function AlertBanner({ alerts, onDismiss }) {
    if (!alerts.length) return null;

    return (
        <div className="flex flex-col gap-2 mb-4">
            {alerts.map((a, i) => (
                <div
                    key={i}
                    className="flex items-center gap-3 bg-red-500/8 border border-red-500/40 border-l-4 border-l-red-500 rounded-lg px-3.5 py-2.5 font-mono"
                >
                    <span className="text-lg">🚨</span>
                    <div className="flex-1">
                        <span className="text-red-400 font-semibold text-xs">ALERT [{a.env}] </span>
                        <span className="text-red-600 text-xs">{a.message}</span>
                    </div>
                    <span className="text-red-400 text-xs mr-2">{a.time}</span>
                    <button
                        onClick={() => onDismiss(i)}
                        className="bg-transparent border-none text-red-400 cursor-pointer text-lg px-1 leading-none hover:text-red-300 transition-colors"
                    >
                        ×
                    </button>
                </div>
            ))}
        </div>
    );
}