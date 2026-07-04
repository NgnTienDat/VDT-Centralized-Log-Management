import { useState, useMemo } from "react";
import FilterBar from "../components/filter/FilterBar.jsx";
import AppFooter from "../components/layout/AppFooter.jsx";
import LogTable from "../components/log/LogTable.jsx";
import LogTableSkeleton from "../components/log/LogTableSkeleton.jsx";
import LogDetail from "../components/log/LogDetail.jsx";
import StatsRow from "../components/stats/StatsRow.jsx";
import { useLogQuery } from "../hooks/useLogQuery.js";
import { useLogStream } from "../hooks/useLogStream.js";
import { useServicesQuery } from "../hooks/useServiceQuery.js";
import { useAppsQuery } from "../hooks/useAppQuery.js";
import { useFilterStore } from "../stores/useFilterStore.js";

export default function LogDashboard({ isDark }) {
    const [selectedLog, setSelectedLog] = useState(null);
    const [liveMode, setLiveMode] = useState(false);
    const [errorHistory] = useState([0, 1, 0, 2, 1, 3, 0, 1, 2, 4, 3, 5, 2, 1, 3, 4]);

    const {
        environment, setEnvironment,
        logLevel, setLogLevel,
        serviceName, setServiceName,
        appName, setAppName,
        q, setQ,
    } = useFilterStore();

    const { services } = useServicesQuery();
    const { apps } = useAppsQuery();

    const {
        logs,
        isLoading,
        isFetching,
        isError,
        error,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage,
    } = useLogQuery();

    useLogStream(liveMode, isFetching);

    const statsMetrics = useMemo(() => {
        if (!logs) return { total: 0, error: 0, warn: 0 };
        return {
            total: logs.length,
            error: logs.filter((l) => l.level === "ERROR" || l.level === "FATAL").length,
            warn: logs.filter((l) => l.level === "WARN" || l.level === "WARNING").length,
        };
    }, [logs]);

    const liveButtonClass = liveMode
        ? "bg-rose-500/12 border-red-500/45 text-rose-400 hover:bg-rose-500/18"
        : isDark
            ? "bg-emerald-500/10 border-emerald-500/35 text-emerald-400 hover:bg-emerald-500/16"
            : "bg-emerald-50 border-emerald-300 text-emerald-700 hover:bg-emerald-100";

    return (
        <div className="flex flex-col gap-1">
            <div className="flex items-center justify-between border-b pb-1 border-slate-200 dark:border-white/5">
                <div>
                    <h2 className="text-base font-bold tracking-tight text-slate-500 dark:text-slate-400">
                        Logs Stream Explorer
                    </h2>
                    <p className="text-xs text-slate-400 mt-0.5">
                        Truy vết dữ liệu log tập trung hệ thống theo thời gian thực
                    </p>
                </div>

                <button
                    onClick={() => setLiveMode((p) => !p)}
                    className={`flex items-center gap-2 border rounded-lg px-3.5 py-1.5 cursor-pointer font-mono text-[11px] font-bold tracking-wider transition-all duration-150 ${liveButtonClass}`}
                >
                    {liveMode ? (
                        <>
                            <span className="w-2 h-2 rounded-full bg-rose-500 animate-pulse" />
                            <span>⏸ PAUSE STREAM</span>
                        </>
                    ) : (
                        <>
                            <span className={`w-2 h-2 rounded-full inline-block ${isDark ? "bg-slate-600" : "bg-slate-400"}`} />
                            <span>▶ GO LIVE</span>
                        </>
                    )}
                </button>
            </div>

            <StatsRow stats={statsMetrics} errorHistory={errorHistory} isDark={isDark} />

            <FilterBar
                isDark={isDark}
                search={q}
                onSearch={setQ}
                filterLevel={logLevel}
                onLevel={setLogLevel}
                filterEnv={environment}
                onEnv={setEnvironment}
                filterService={serviceName}
                onService={setServiceName}
                filterApp={appName}
                onApp={setAppName}
                services={services}
                apps={apps}
                filteredCount={logs.length}
                totalCount={logs.length}
            />

            <div className="flex flex-col gap-4">
                {isLoading ? (
                    <div className={`border rounded-xl p-4 ${isDark ? "bg-[#0a0f1a] border-white/6" : "bg-white border-slate-200 shadow-sm"}`}>
                        <LogTableSkeleton isDark={isDark} />
                    </div>
                ) : isError ? (
                    <div className={`border rounded-xl p-8 text-center font-mono text-xs ${isDark ? "bg-[#0a0f1a] border-white/6 text-red-400" : "bg-white border-slate-200 text-red-600 shadow-sm"}`}>
                        Error loading logs: {error?.message || "Unknown error"}
                    </div>
                ) : (
                    <LogTable
                        logs={logs}
                        selectedLog={selectedLog}
                        onSelectLog={setSelectedLog}
                        isDark={isDark}
                        hasNextPage={hasNextPage}
                        isFetchingNextPage={isFetchingNextPage}
                        fetchNextPage={fetchNextPage}
                    />
                )}
            </div>

            {selectedLog && (
                <LogDetail log={selectedLog} onClose={() => setSelectedLog(null)} isDark={isDark} />
            )}

            <AppFooter logs={logs} isDark={isDark} />
        </div>
    );
}