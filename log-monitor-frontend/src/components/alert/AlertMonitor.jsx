import { useState, useCallback, useRef } from "react";
import { useAlerts } from "../../hooks/useAlerts";
import AlertCard from "./AlertCard";
import AlertStats from "./AlertStats";
import AlertRawPanel from "./AlertRawPanel";
import AlertConnectionLog from "./AlertConnectionLog";

const EMPTY_STATS = { total: 0, CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0 };

function nowTime() {
    return new Date().toLocaleTimeString("vi-VN", { hour12: false });
}

/**
 * AlertMonitor — full alert monitor section.
 * Drop this anywhere in your page layout; it self-connects via useAlerts.
 */
export default function AlertMonitor({ isDark }) {
    const [alerts, setAlerts] = useState([]);
    const [stats, setStats] = useState({ ...EMPTY_STATS });
    const [latestRaw, setLatestRaw] = useState(null);
    const [connLog, setConnLog] = useState([]);
    const [isOpen, setIsOpen] = useState(true);

    const addLog = useCallback((msg, type = "info") => {
        setConnLog((prev) => [
            ...prev.slice(-49), // keep last 50 lines
            { ts: nowTime(), msg, type },
        ]);
    }, []);

    const handleAlert = useCallback(
        (data) => {
            const sev = (data.severity || "MEDIUM").toUpperCase();
            addLog(`Alert received: rule=${data.ruleId ?? "—"}, sev=${sev}`, "ok");
            setLatestRaw(data);
            setAlerts((prev) => [data, ...prev].slice(0, 100)); // cap at 100 cards
            setStats((prev) => ({
                ...prev,
                total: prev.total + 1,
                [sev]: (prev[sev] ?? 0) + 1,
            }));
        },
        [addLog]
    );

    // Self-connecting hook — re-subscribes when environment/serviceName filter changes
    useAlerts(handleAlert);

    const clearAll = () => {
        setAlerts([]);
        setStats({ ...EMPTY_STATS });
        setLatestRaw(null);
    };

    return (
        <section className={`mt-6 rounded-2xl border overflow-hidden
            ${isDark ? "bg-[#0a0f1a] border-white/6" : "bg-white border-slate-200 shadow-sm"}`}
        >
            {/* ── Section header ── */}
            <div className={`flex items-center justify-between px-5 py-3 border-b
                ${isDark ? "border-white/6" : "border-slate-200"}`}
            >
                <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-linear-to-br from-violet-600 to-indigo-500 flex items-center justify-center text-base">
                        🔔
                    </div>
                    <div>
                        <h2 className={`text-sm font-semibold tracking-tight ${isDark ? "text-slate-200" : "text-slate-800"}`}>
                            Alert <span className="text-violet-400">Monitor</span>
                        </h2>
                        <p className={`text-[10px] ${isDark ? "text-slate-500" : "text-slate-400"}`}>
                            Real-time · /topic/alerts
                        </p>
                    </div>
                </div>

                <div className="flex items-center gap-2">
                    {/* Alert count chip */}
                    {stats.total > 0 && (
                        <span className="bg-violet-500 text-white text-[11px] font-bold px-2.5 py-0.5 rounded-full tabular-nums">
                            {stats.total}
                        </span>
                    )}
                    <button
                        onClick={clearAll}
                        className={`text-xs px-3 py-1.5 rounded-lg transition-colors
                            ${isDark
                                ? "bg-white/5 text-slate-400 hover:bg-white/10 hover:text-slate-200"
                                : "bg-slate-100 text-slate-500 hover:bg-slate-200 hover:text-slate-700"
                            }`}
                    >
                        Xoá tất cả
                    </button>
                    <button
                        onClick={() => setIsOpen((p) => !p)}
                        className={`text-xs px-3 py-1.5 rounded-lg transition-colors
                            ${isDark
                                ? "bg-white/5 text-slate-400 hover:bg-white/10"
                                : "bg-slate-100 text-slate-500 hover:bg-slate-200"
                            }`}
                    >
                        {isOpen ? "Thu gọn ▲" : "Mở rộng ▼"}
                    </button>
                </div>
            </div>

            {/* ── Collapsible body ── */}
            {isOpen && (
                <div className="grid grid-cols-[1fr_280px] gap-0">

                    {/* ── Alert feed ── */}
                    <div className={`p-5 flex flex-col gap-3 max-h-130 overflow-y-auto border-r
                        ${isDark ? "border-white/6" : "border-slate-200"}`}
                    >
                        {alerts.length === 0 ? (
                            <div className="flex flex-col items-center justify-center h-48 gap-3">
                                <span className="text-4xl opacity-30">📭</span>
                                <p className={`text-xs ${isDark ? "text-slate-600" : "text-slate-400"}`}>
                                    Đang lắng nghe <code className="font-mono">/topic/alerts</code>…
                                </p>
                            </div>
                        ) : (
                            alerts.map((a, i) => (
                                <AlertCard key={i} alert={a} isDark={isDark} />
                            ))
                        )}
                    </div>

                    {/* ── Sidebar ── */}
                    <div className="p-4 flex flex-col gap-4 max-h-130 overflow-y-auto">
                        <AlertStats stats={stats} isDark={isDark} />
                        <AlertRawPanel raw={latestRaw} isDark={isDark} />
                        {/* <AlertConnectionLog lines={connLog} isDark={isDark} /> */}
                    </div>
                </div>
            )}
        </section>
    );
}
