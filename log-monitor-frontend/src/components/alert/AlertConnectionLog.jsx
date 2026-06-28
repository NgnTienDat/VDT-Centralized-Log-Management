/**
 * AlertConnectionLog — tiny inline log showing STOMP connection lifecycle events.
 * Lines are colored by type: ok (green), err (red), info (blue).
 */
export default function AlertConnectionLog({ lines, isDark }) {
    const COLOR = {
        ok:   "text-green-400",
        err:  "text-red-400",
        info: "text-sky-400",
    };

    return (
        <div className={`rounded-xl border overflow-hidden ${isDark ? "bg-[#151821] border-white/6" : "bg-white border-slate-200"}`}>
            <div className={`px-3 py-2 text-[10px] font-bold uppercase tracking-widest border-b
                ${isDark ? "text-slate-500 border-white/6" : "text-slate-400 border-slate-200"}`}>
                📋 Connection log
            </div>
            <div className="font-mono text-[10px] p-3 max-h-36 overflow-y-auto leading-7">
                {lines.length === 0 ? (
                    <span className="text-slate-600">— awaiting connection —</span>
                ) : (
                    lines.map((l, i) => (
                        <div key={i} className="flex gap-2">
                            <span className="text-slate-600 shrink-0">[{l.ts}]</span>
                            <span className={COLOR[l.type] ?? "text-slate-400"}>{l.msg}</span>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
}
