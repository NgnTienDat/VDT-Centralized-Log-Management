import { useState, useEffect, useRef } from "react";
import { SERVICES, formatTs } from "../utils/constants.js";

import AlertBanner from "../components/alert/AlertBanner.jsx";
import FilterBar from "../components/filter/FilterBar.jsx";
import AppHeader from "../components/layout/AppHeader.jsx";
import AppFooter from "../components/layout/AppFooter.jsx";
import LogTable from "../components/log/LogTable.jsx";
import LogTableSkeleton from "../components/log/LogTableSkeleton.jsx";
import LogDetail from "../components/log/LogDetail.jsx";
import StatsRow from "../components/stats/StatsRow.jsx";
import { useLogQuery } from "../hooks/useLogQuery.js";
import { useLogStream } from "../hooks/useLogStream.js";
import { useFilterStore } from "../stores/useFilterStore.js";
import { useServicesQuery } from "../hooks/useServiceQuery.js";

export default function LogDashboard() {
    const [isDark, setIsDark] = useState(true);
    const [selectedLog, setSelectedLog] = useState(null);
    const [liveMode, setLiveMode] = useState(false);
    const [alerts, setAlerts] = useState([]);
    const [errorHistory] = useState([0, 1, 0, 2, 1, 3, 0, 1, 2, 4, 3, 5, 2, 1, 3, 4, 2, 5, 6, 4]);

    const seenLogIds = useRef(new Set());

    // Connect filter store and custom log query hook
    const {
        environment,
        logLevel,
        serviceName,
        q,
        setEnvironment,
        setLogLevel,
        setServiceName,
        setQ
    } = useFilterStore();
    const { services } = useServicesQuery();
    const {
        logs,
        isLoading,
        isFetching,
        isError,
        error,
        isFetchingNextPage,
        hasNextPage,
        fetchNextPage
    } = useLogQuery();

    useLogStream(liveMode, isFetching);

    // Compute stats from dynamically fetched logs
    const stats = {
        total: logs.length,
        error: logs.filter((l) => l.level === "ERROR").length,
        warn: logs.filter((l) => l.level === "WARN").length,
    };

    // Live alert monitoring: scan new logs for ERROR state when liveMode is active
    useEffect(() => {
        if (!liveMode) {
            // Keep track of logs to avoid alerting when live mode gets turned on
            logs.forEach((l) => seenLogIds.current.add(l.id));
            return;
        }

        logs.forEach((l) => {
            if (!seenLogIds.current.has(l.id)) {
                seenLogIds.current.add(l.id);
                if (l.level === "ERROR") {
                    setAlerts((prev) =>
                        [
                            {
                                env: l.env,
                                message: `New ERROR from ${l.service}: ${l.message}`,
                                time: formatTs(l.timestamp),
                            },
                            ...prev,
                        ].slice(0, 5)
                    );
                }
            }
        });
    }, [logs, liveMode]);

    return (
        <div
            className={`min-h-screen font-mono pb-10 transition-colors duration-200 ${isDark ? "bg-[#080c14] text-slate-300" : "bg-slate-100 text-slate-800"
                }`}
        >
            {/* Scrollbar styles injected globally */}
            <style>{`
                @import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;700&display=swap');
                * { box-sizing: border-box; }
                ::-webkit-scrollbar { width: 6px; height: 6px; }
                ::-webkit-scrollbar-track { background: transparent; }
                ::-webkit-scrollbar-thumb {
                background: ${isDark ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.12)"};
                border-radius: 3px;
                }`}
            </style>

            <AppHeader
                isDark={isDark}
                onToggleTheme={() => setIsDark((p) => !p)}
                liveMode={liveMode}
                onToggleLive={() => setLiveMode((p) => !p)}
                alertCount={alerts.length}
            />

            <div className="px-6 pt-5">
                <AlertBanner
                    alerts={alerts}
                    onDismiss={(i) => setAlerts((prev) => prev.filter((_, idx) => idx !== i))}
                />

                <StatsRow stats={stats} errorHistory={errorHistory} isDark={isDark} />

                <FilterBar
                    search={q || ""} onSearch={setQ}
                    filterLevel={logLevel || "ALL"} onLevel={setLogLevel}
                    filterEnv={environment || "ALL"} onEnv={setEnvironment}
                    filterService={serviceName || "ALL"} onService={setServiceName}
                    filteredCount={logs.length} totalCount={logs.length}
                    isDark={isDark}
                    services={services}
                />

                <div className="mt-1">
                    {isLoading ? (
                        <div className={`border rounded-xl overflow-hidden ${isDark ? "bg-[#0a0f1a] border-white/6" : "bg-white border-slate-200 shadow-sm"
                            }`}>
                            <LogTableSkeleton isDark={isDark} />
                        </div>
                    ) : isError ? (
                        <div className={`border rounded-xl p-8 text-center font-mono text-xs ${isDark ? "bg-[#0a0f1a] border-white/6 text-red-400" : "bg-white border-slate-200 text-red-600 shadow-sm"
                            }`}>
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
                    <LogDetail
                        log={selectedLog}
                        onClose={() => setSelectedLog(null)}
                        isDark={isDark}
                    />
                )}

                <AppFooter logs={logs} isDark={isDark} />
            </div>
        </div>
    );
}