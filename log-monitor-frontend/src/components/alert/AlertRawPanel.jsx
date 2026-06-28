/**
 * AlertRawPanel — shows the latest raw JSON payload received from /topic/alerts.
 */
export default function AlertRawPanel({ raw, isDark }) {
    return (
        <div className={`rounded-xl border overflow-hidden ${isDark ? "bg-[#151821] border-white/6" : "bg-white border-slate-200"}`}>
            <div className={`px-3 py-2 text-[10px] font-bold uppercase tracking-widest border-b
                ${isDark ? "text-slate-500 border-white/6" : "text-slate-400 border-slate-200"}`}>
                🗂 Raw JSON (gần nhất)
            </div>
            <pre
                className="font-mono text-[10px] text-sky-400 p-3 whitespace-pre-wrap break-all max-h-64 overflow-y-auto leading-relaxed"
            >
                {raw ? JSON.stringify(raw, null, 2) : "— chưa có dữ liệu —"}
            </pre>
        </div>
    );
}
