import { useRef } from "react";
import LogRow from "./LogRow.jsx";
import LogRowV2 from "./LogRowV2.jsx";

const COLUMNS = ["TIME", "LEVEL", "ENV", "SERVICE", "MESSAGE"];

export default function LogTable({
    logs,
    selectedLog,
    onSelectLog,
    isDark,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
}) {
    const listRef = useRef(null);

    const panelClass = isDark ? "bg-[#0a0f1a] border-white/6" : "bg-white border-slate-200 shadow-sm";
    const headerClass = isDark ? "bg-white/2 border-white/6" : "bg-slate-50 border-slate-200";

    return (
        <div className={`border rounded-xl overflow-hidden transition-colors duration-200 ${panelClass}`}>
            {/* Column headers V2 */}
            {/* <div
                className={`grid gap-x-3 px-3.5 py-2 border-b ${headerClass}`}
                style={{ gridTemplateColumns: "90px 62px 88px 140px 1fr" }}
            >
                {COLUMNS.map((h) => (
                    <span
                        key={h}
                        className="text-[10px] tracking-widest font-bold text-slate-700 uppercase font-mono"
                    >
                        {h}
                    </span>
                ))}
            </div> */}

            {/* Column headers V1 */}
            <div 
                className={`grid gap-x-3 px-3.5 py-2 border-b border-l-[3px] border-l-transparent ${headerClass}`}
                style={{ gridTemplateColumns: "148px 62px 88px 140px 1fr" }}
            >
                {COLUMNS.map((h) => (
                    <span
                        key={h}
                        className="text-[10px] tracking-widest font-bold text-slate-700 uppercase font-mono"
                    >
                        {h}
                    </span>
                ))}
            </div>

            {/* Rows */}
            <div ref={listRef} className="max-h-120 overflow-y-auto">
                {logs.length === 0 ? (
                    <div className="py-10 text-center text-slate-700 text-sm font-mono">
                        No logs match current filters
                    </div>
                ) : (
                    <>
                        {logs.map((log) => (
                            <LogRow
                                key={log.docId || log.id}
                                log={log}
                                onClick={onSelectLog}
                                selected={selectedLog?.id === log.id}
                                isDark={isDark}
                            />
                            // <LogRowV2 key={log.docId || log.id} log={log} onClick={onSelectLog} selected={selectedLog?.id === log.id} isDark={isDark} />
                        ))}

                        {/* Load More Button / Loading Indicator */}
                        {hasNextPage && (
                            <div className={`p-4 border-t text-center ${isDark ? "border-white/4" : "border-slate-100"}`}>
                                {isFetchingNextPage ? (
                                    <span className="text-[10px] tracking-widest font-bold text-sky-400 animate-pulse font-mono">
                                        LOADING MORE LOGS...
                                    </span>
                                ) : (
                                    <button
                                        onClick={fetchNextPage}
                                        className={`px-5 py-2 text-[10px] tracking-widest font-bold rounded-lg border transition-all duration-200 cursor-pointer font-mono ${
                                            isDark
                                                ? "bg-sky-500/10 border-sky-500/30 text-sky-400 hover:bg-sky-500/20 hover:border-sky-500/40"
                                                : "bg-sky-50 border-sky-200 text-sky-600 hover:bg-sky-100 hover:border-sky-300"
                                        }`}
                                    >
                                        LOAD MORE
                                    </button>
                                )}
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}

// Expose the internal ref for parent scroll control
export { LogTable };