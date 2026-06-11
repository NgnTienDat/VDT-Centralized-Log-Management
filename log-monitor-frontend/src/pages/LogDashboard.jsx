import { useState, useEffect, useRef, useCallback } from "react";

const ENVIRONMENTS = ["ALL", "DEV", "STAGING", "TEST", "PROD"];
const LOG_LEVELS = ["ALL", "INFO", "WARN", "ERROR", "DEBUG"];
const SERVICES = ["ALL", "auth-service", "chat-service", "user-service", "payment-service", "notification-service"];

const MOCK_LOGS = [
    { id: 1, timestamp: "2025-06-05T08:01:12.412Z", level: "ERROR", env: "DEV", service: "auth-service", message: "NullPointerException at AuthController.java:142 — user token is null", traceId: "a1b2c3d4", thread: "http-nio-8080-exec-3" },
    { id: 2, timestamp: "2025-06-05T08:01:13.001Z", level: "ERROR", env: "DEV", service: "auth-service", message: "Failed to validate JWT: signature does not match", traceId: "a1b2c3d4", thread: "http-nio-8080-exec-3" },
    { id: 3, timestamp: "2025-06-05T08:01:14.880Z", level: "WARN", env: "STAGING", service: "payment-service", message: "Payment gateway timeout after 5000ms, retrying... (attempt 2/3)", traceId: "f9e8d7c6", thread: "payment-executor-1" },
    { id: 4, timestamp: "2025-06-05T08:01:15.321Z", level: "INFO", env: "DEV", service: "chat-service", message: "WebSocket connection established for user #88421", traceId: "b3c4d5e6", thread: "ws-handler-12" },
    { id: 5, timestamp: "2025-06-05T08:01:16.001Z", level: "DEBUG", env: "TEST", service: "user-service", message: "Cache HIT for key: user:profile:88421, TTL remaining: 287s", traceId: "c5d6e7f8", thread: "redis-pool-2" },
    { id: 6, timestamp: "2025-06-05T08:01:17.512Z", level: "ERROR", env: "STAGING", service: "notification-service", message: "SMTP connection refused — host: smtp.internal:587, check firewall rules", traceId: "d7e8f9a0", thread: "mail-sender-1" },
    { id: 7, timestamp: "2025-06-05T08:01:18.004Z", level: "INFO", env: "DEV", service: "user-service", message: "User #88421 updated profile successfully, 3 fields changed", traceId: "e9f0a1b2", thread: "http-nio-8080-exec-7" },
    { id: 8, timestamp: "2025-06-05T08:01:19.200Z", level: "WARN", env: "DEV", service: "chat-service", message: "Message queue depth: 1842 — approaching threshold of 2000", traceId: "f1a2b3c4", thread: "queue-monitor-1" },
    { id: 9, timestamp: "2025-06-05T08:01:20.777Z", level: "ERROR", env: "TEST", service: "payment-service", message: "Database connection pool exhausted — all 20 connections in use, request queued", traceId: "a3b4c5d6", thread: "db-pool-monitor" },
    { id: 10, timestamp: "2025-06-05T08:01:21.333Z", level: "INFO", env: "STAGING", service: "auth-service", message: "Session cleanup completed — 142 expired sessions removed", traceId: "b5c6d7e8", thread: "scheduler-1" },
    { id: 11, timestamp: "2025-06-05T08:01:22.001Z", level: "DEBUG", env: "DEV", service: "payment-service", message: "Transaction rollback initiated — reason: constraint violation on orders table", traceId: "c7d8e9f0", thread: "tx-manager-3" },
    { id: 12, timestamp: "2025-06-05T08:01:23.444Z", level: "WARN", env: "TEST", service: "auth-service", message: "Rate limit almost reached for IP 192.168.1.105 — 95/100 requests in 60s window", traceId: "d9e0f1a2", thread: "rate-limiter-1" },
    { id: 13, timestamp: "2025-06-05T08:01:24.888Z", level: "ERROR", env: "DEV", service: "notification-service", message: "Failed to send push notification — FCM responded: InvalidRegistration for token abc123xyz", traceId: "e1f2a3b4", thread: "push-sender-2" },
    { id: 14, timestamp: "2025-06-05T08:01:25.100Z", level: "INFO", env: "DEV", service: "chat-service", message: "Room #5512 archived after 30 days of inactivity", traceId: "f3a4b5c6", thread: "archival-job-1" },
    { id: 15, timestamp: "2025-06-05T08:01:26.200Z", level: "INFO", env: "STAGING", service: "user-service", message: "Bulk import completed — 1024 records processed, 3 skipped", traceId: "a5b6c7d8", thread: "import-executor-1" },
    { id: 16, timestamp: "2025-06-05T08:01:27.600Z", level: "ERROR", env: "STAGING", service: "auth-service", message: "StackOverflowError in recursive token refresh loop — possible circular dependency", traceId: "b7c8d9e0", thread: "http-nio-8080-exec-1" },
    { id: 17, timestamp: "2025-06-05T08:01:28.010Z", level: "WARN", env: "DEV", service: "user-service", message: "Slow query detected: 4821ms for SELECT * FROM audit_logs — missing index on created_at", traceId: "c9d0e1f2", thread: "sql-monitor-1" },
    { id: 18, timestamp: "2025-06-05T08:01:29.500Z", level: "DEBUG", env: "STAGING", service: "chat-service", message: "Reconnect attempt 1/5 for WebSocket session ws_9981", traceId: "d1e2f3a4", thread: "ws-reconnect-pool" },
    { id: 19, timestamp: "2025-06-05T08:01:30.333Z", level: "ERROR", env: "TEST", service: "chat-service", message: "Message delivery failed — recipient user #99012 not found in active sessions", traceId: "e3f4a5b6", thread: "msg-dispatcher-4" },
    { id: 20, timestamp: "2025-06-05T08:01:31.222Z", level: "INFO", env: "DEV", service: "payment-service", message: "Stripe webhook received — event: payment_intent.succeeded, amount: 250000 VND", traceId: "f5a6b7c8", thread: "webhook-handler-1" },
];

// ─── Theme tokens ────────────────────────────────────────────────────────────
const THEMES = {
    dark: {
        name: "dark",
        bg: "#080c14",
        bgHeader: "#0a0f1a",
        bgCard: "rgba(255,255,255,0.03)",
        bgPanel: "#0a0f1a",
        bgInput: "rgba(255,255,255,0.04)",
        bgRowHover: "rgba(255,255,255,0.03)",
        bgRowError: "rgba(255,77,77,0.04)",
        bgRowSel: "rgba(255,255,255,0.05)",
        bgColHead: "rgba(255,255,255,0.02)",
        border: "rgba(255,255,255,0.06)",
        borderInput: "rgba(255,255,255,0.08)",
        borderRow: "rgba(255,255,255,0.04)",
        borderColH: "rgba(255,255,255,0.06)",
        textPrimary: "#c8cfe8",
        textSecond: "#94a3b8",
        textMuted: "#6b7a99",
        textDim: "#4a5568",
        textFaint: "#2d3748",
        textMsg: "#c8cfe8",
        textMsgErr: "#ffb3b3",
        textMsgWarn: "#ffe4a0",
        accentInfo: "#4dd2ff",
        searchIcon: "#4a5568",
        footerText: "#2d3748",
    },
    light: {
        name: "light",
        bg: "#f0f2f7",
        bgHeader: "#ffffff",
        bgCard: "#ffffff",
        bgPanel: "#ffffff",
        bgInput: "#f8f9fc",
        bgRowHover: "#f5f7fc",
        bgRowError: "rgba(255,77,77,0.04)",
        bgRowSel: "#eef2ff",
        bgColHead: "#f5f7fc",
        border: "#e2e6ef",
        borderInput: "#d4d9e8",
        borderRow: "#eef0f6",
        borderColH: "#e2e6ef",
        textPrimary: "#1a2035",
        textSecond: "#374151",
        textMuted: "#6b7a99",
        textDim: "#9299b0",
        textFaint: "#b4bacf",
        textMsg: "#2d3748",
        textMsgErr: "#c0152a",
        textMsgWarn: "#92580a",
        accentInfo: "#0284c7",
        searchIcon: "#9299b0",
        footerText: "#b4bacf",
    },
};

const levelConfig = {
    ERROR: { color: "#ff4d4d", bg: "rgba(255,77,77,0.12)", border: "rgba(255,77,77,0.35)" },
    WARN: { color: "#f59e0b", bg: "rgba(245,158,11,0.12)", border: "rgba(245,158,11,0.35)" },
    INFO: { color: "#0ea5e9", bg: "rgba(14,165,233,0.10)", border: "rgba(14,165,233,0.30)" },
    DEBUG: { color: "#8b95b0", bg: "rgba(139,149,176,0.10)", border: "rgba(139,149,176,0.25)" },
};

const envConfig = {
    DEV: { color: "#7c3aed", bg: "rgba(124,58,237,0.10)" },
    STAGING: { color: "#059669", bg: "rgba(5,150,105,0.10)" },
    TEST: { color: "#d97706", bg: "rgba(217,119,6,0.10)" },
    PROD: { color: "#db2777", bg: "rgba(219,39,119,0.10)" },
};

function formatTs(ts) {
    const d = new Date(ts);
    return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}:${String(d.getSeconds()).padStart(2, "0")}.${String(d.getMilliseconds()).padStart(3, "0")}`;
}

// ─── Theme Toggle Button ──────────────────────────────────────────────────────
function ThemeToggle({ isDark, onToggle, t }) {
    return (
        <button onClick={onToggle} title={isDark ? "Switch to Light mode" : "Switch to Dark mode"} style={{
            background: t.bgCard,
            border: `1px solid ${t.border}`,
            borderRadius: 8,
            padding: "6px 12px",
            cursor: "pointer",
            display: "flex", alignItems: "center", gap: 8,
            transition: "all 0.2s",
            color: t.textMuted,
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: 11,
            letterSpacing: "0.04em",
        }}>
            <span style={{ fontSize: 15, lineHeight: 1 }}>{isDark ? "☀️" : "🌙"}</span>
            <span style={{ color: t.textDim }}>{isDark ? "LIGHT" : "DARK"}</span>
        </button>
    );
}

// ─── Alert Banner ─────────────────────────────────────────────────────────────
function AlertBanner({ alerts, onDismiss }) {
    if (!alerts.length) return null;
    return (
        <div style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 16 }}>
            {alerts.map((a, i) => (
                <div key={i} style={{
                    background: "rgba(255,77,77,0.08)", border: "1px solid rgba(255,77,77,0.4)",
                    borderLeft: "4px solid #ff4d4d", borderRadius: 8, padding: "10px 14px",
                    display: "flex", alignItems: "center", gap: 12,
                    fontFamily: "'JetBrains Mono', monospace",
                }}>
                    <span style={{ fontSize: 18 }}>🚨</span>
                    <div style={{ flex: 1 }}>
                        <span style={{ color: "#ff4d4d", fontWeight: 600, fontSize: 12 }}>ALERT [{a.env}] </span>
                        <span style={{ color: "#c0152a", fontSize: 12 }}>{a.message}</span>
                    </div>
                    <span style={{ color: "#ff4d4d", fontSize: 11, marginRight: 8 }}>{a.time}</span>
                    <button onClick={() => onDismiss(i)} style={{ background: "transparent", border: "none", color: "#ff4d4d", cursor: "pointer", fontSize: 18, padding: "0 4px", lineHeight: 1 }}>×</button>
                </div>
            ))}
        </div>
    );
}

// ─── Stat Card ────────────────────────────────────────────────────────────────
function StatCard({ label, value, color, sublabel, t }) {
    return (
        <div style={{
            background: t.bgCard, border: `1px solid ${t.border}`,
            borderRadius: 10, padding: "14px 18px", minWidth: 0,
            transition: "background 0.25s, border 0.25s",
        }}>
            <div style={{ fontSize: 11, color: t.textMuted, fontFamily: "'JetBrains Mono', monospace", letterSpacing: "0.08em", textTransform: "uppercase", marginBottom: 6 }}>{label}</div>
            <div style={{ fontSize: 28, fontWeight: 700, color, fontFamily: "'JetBrains Mono', monospace", lineHeight: 1 }}>{value}</div>
            {sublabel && <div style={{ fontSize: 11, color: t.textDim, marginTop: 4 }}>{sublabel}</div>}
        </div>
    );
}

// ─── Mini Bar Chart ───────────────────────────────────────────────────────────
function MiniChart({ data, t }) {
    const max = Math.max(...data, 1);
    return (
        <div style={{ display: "flex", alignItems: "flex-end", gap: 3, height: 40 }}>
            {data.map((v, i) => (
                <div key={i} style={{
                    flex: 1,
                    background: v > 0 ? `rgba(255,77,77,${0.3 + (v / max) * 0.7})` : t.borderRow,
                    borderRadius: 2,
                    height: `${Math.max((v / max) * 100, 4)}%`,
                    transition: "height 0.3s",
                }} title={`${v} errors`} />
            ))}
        </div>
    );
}

// ─── Log Row ──────────────────────────────────────────────────────────────────
function LogRow({ log, onClick, selected, t }) {
    const lvl = levelConfig[log.level] || levelConfig.INFO;
    const msgColor = log.level === "ERROR" ? t.textMsgErr : log.level === "WARN" ? t.textMsgWarn : t.textMsg;
    return (
        <div onClick={() => onClick(log)} style={{
            display: "grid", gridTemplateColumns: "90px 62px 88px 140px 1fr",
            gap: "0 12px", alignItems: "start", padding: "7px 14px", cursor: "pointer",
            background: selected ? t.bgRowSel : log.level === "ERROR" ? t.bgRowError : "transparent",
            borderLeft: selected ? `3px solid ${lvl.color}` : "3px solid transparent",
            borderBottom: `1px solid ${t.borderRow}`,
            transition: "background 0.1s",
        }}
            onMouseEnter={e => { if (!selected) e.currentTarget.style.background = log.level === "ERROR" ? t.bgRowError : t.bgRowHover; }}
            onMouseLeave={e => { if (!selected) e.currentTarget.style.background = log.level === "ERROR" ? t.bgRowError : "transparent"; }}
        >
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 11, color: t.textDim, whiteSpace: "nowrap" }}>{formatTs(log.timestamp)}</span>
            <span style={{
                fontFamily: "'JetBrains Mono', monospace", fontSize: 10, fontWeight: 700,
                color: lvl.color, background: lvl.bg, border: `1px solid ${lvl.border}`,
                borderRadius: 4, padding: "1px 6px", textAlign: "center", letterSpacing: "0.04em", whiteSpace: "nowrap",
            }}>{log.level}</span>
            <span style={{
                fontFamily: "'JetBrains Mono', monospace", fontSize: 10,
                color: envConfig[log.env]?.color || t.textMuted,
                background: envConfig[log.env]?.bg || "transparent",
                borderRadius: 4, padding: "1px 6px", whiteSpace: "nowrap",
            }}>{log.env}</span>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 11, color: t.textDim, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{log.service}</span>
            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: 12, color: msgColor, lineHeight: 1.5, wordBreak: "break-word" }}>{log.message}</span>
        </div>
    );
}

// ─── Log Detail Panel ─────────────────────────────────────────────────────────
function LogDetail({ log, onClose, t }) {
    if (!log) return null;
    const lvl = levelConfig[log.level] || levelConfig.INFO;
    return (
        <div style={{
            background: t.bgPanel, border: `1px solid ${t.border}`,
            borderRadius: 10, padding: 20,
            fontFamily: "'JetBrains Mono', monospace", fontSize: 12, marginTop: 16,
            transition: "background 0.25s, border 0.25s",
        }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
                <span style={{ color: lvl.color, fontWeight: 700, fontSize: 13 }}>▶ Log Detail</span>
                <button onClick={onClose} style={{ background: "transparent", border: "none", color: t.textMuted, cursor: "pointer", fontSize: 20, lineHeight: 1, padding: "0 4px" }}>×</button>
            </div>
            {[
                ["timestamp", log.timestamp],
                ["level", log.level],
                ["environment", log.env],
                ["service", log.service],
                ["thread", log.thread],
                ["traceId", log.traceId],
                ["message", log.message],
            ].map(([k, v]) => (
                <div key={k} style={{ display: "grid", gridTemplateColumns: "110px 1fr", gap: 8, marginBottom: 8, borderBottom: `1px solid ${t.borderRow}`, paddingBottom: 8 }}>
                    <span style={{ color: t.textDim }}>{k}</span>
                    <span style={{ color: k === "level" ? lvl.color : k === "message" ? t.textPrimary : t.textSecond, wordBreak: "break-all" }}>{v}</span>
                </div>
            ))}
        </div>
    );
}

// ─── Main Dashboard ───────────────────────────────────────────────────────────
export default function LogDashboard() {
    const [isDark, setIsDark] = useState(true);
    const t = THEMES[isDark ? "dark" : "light"];

    const [logs, setLogs] = useState(MOCK_LOGS);
    const [filterLevel, setFilterLevel] = useState("ALL");
    const [filterEnv, setFilterEnv] = useState("ALL");
    const [filterService, setFilterService] = useState("ALL");
    const [search, setSearch] = useState("");
    const [selectedLog, setSelectedLog] = useState(null);
    const [liveMode, setLiveMode] = useState(false);
    const [alerts, setAlerts] = useState([
        { env: "DEV", message: "5 ERROR logs from auth-service in the last 60s — possible auth loop", time: "08:01:14" },
        { env: "STAGING", message: "SMTP connection refused — notification-service cannot send emails", time: "08:01:17" },
    ]);
    const [errorHistory] = useState([0, 1, 0, 2, 1, 3, 0, 1, 2, 4, 3, 5, 2, 1, 3, 4, 2, 5, 6, 4]);
    const listRef = useRef(null);
    const counterRef = useRef(21);

    const filtered = logs.filter(l => {
        if (filterLevel !== "ALL" && l.level !== filterLevel) return false;
        if (filterEnv !== "ALL" && l.env !== filterEnv) return false;
        if (filterService !== "ALL" && l.service !== filterService) return false;
        if (search && !l.message.toLowerCase().includes(search.toLowerCase())
            && !l.service.includes(search.toLowerCase())
            && !l.traceId.includes(search)) return false;
        return true;
    });

    const stats = {
        total: logs.length,
        error: logs.filter(l => l.level === "ERROR").length,
        warn: logs.filter(l => l.level === "WARN").length,
    };

    const addLiveLog = useCallback(() => {
        const lvls = ["INFO", "INFO", "INFO", "WARN", "ERROR", "DEBUG"];
        const envs = ["DEV", "STAGING", "TEST"];
        const svcs = SERVICES.slice(1);
        const msgs = {
            INFO: ["Healthcheck OK — 200 in 12ms", "Scheduled job completed", "Cache refreshed for 512 entries"],
            WARN: ["Response time degraded: 1240ms", "Memory at 78% — consider scaling", "Retry #2 for downstream call"],
            ERROR: ["Connection reset by peer", "Unhandled exception in pipeline", "Timeout waiting for DB lock after 3000ms"],
            DEBUG: ["SQL query executed in 44ms", "Cache MISS — fetching from DB", "Thread pool queue: 12 pending"],
        };
        const level = lvls[Math.floor(Math.random() * lvls.length)];
        const env = envs[Math.floor(Math.random() * envs.length)];
        const svc = svcs[Math.floor(Math.random() * svcs.length)];
        const msgArr = msgs[level];
        const newLog = {
            id: counterRef.current++,
            timestamp: new Date().toISOString(),
            level, env, service: svc,
            message: msgArr[Math.floor(Math.random() * msgArr.length)],
            traceId: Math.random().toString(16).slice(2, 10),
            thread: `http-nio-8080-exec-${Math.floor(Math.random() * 20) + 1}`,
        };
        setLogs(prev => [newLog, ...prev].slice(0, 200));
        if (level === "ERROR") {
            setAlerts(prev => [{
                env,
                message: `New ERROR from ${svc}: ${newLog.message}`,
                time: formatTs(newLog.timestamp),
            }, ...prev].slice(0, 5));
        }
    }, []);

    useEffect(() => {
        if (!liveMode) return;
        const t = setInterval(addLiveLog, 1500 + Math.random() * 1000);
        return () => clearInterval(t);
    }, [liveMode, addLiveLog]);

    useEffect(() => {
        if (liveMode && listRef.current) listRef.current.scrollTop = 0;
    }, [logs, liveMode]);

    // ── CSS string injected via style tag ──────────────────────────────────────
    const css = `
    @import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;700&display=swap');
    * { box-sizing: border-box; margin: 0; padding: 0; }
    ::-webkit-scrollbar { width: 6px; height: 6px; }
    ::-webkit-scrollbar-track { background: transparent; }
    ::-webkit-scrollbar-thumb { background: ${isDark ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.12)"}; border-radius: 3px; }
    .lr-input::placeholder { color: ${t.searchIcon}; }
    .lr-filter-btn {
      background: transparent;
      border: 1px solid ${t.borderInput};
      color: ${t.textMuted};
      border-radius: 6px; padding: 5px 12px; cursor: pointer;
      font-family: 'JetBrains Mono', monospace; font-size: 11px;
      transition: all 0.15s; letter-spacing: 0.04em;
    }
    .lr-filter-btn:hover { border-color: ${isDark ? "rgba(255,255,255,0.22)" : "#aab0c8"}; color: ${t.textPrimary}; }
    .lr-filter-btn.a-all   { background: ${isDark ? "rgba(255,255,255,0.08)" : "#e8ebf4"}; border-color: ${isDark ? "rgba(255,255,255,0.2)" : "#c0c7db"}; color: ${t.textPrimary}; }
    .lr-filter-btn.a-error { background: rgba(255,77,77,0.12); border-color: rgba(255,77,77,0.4); color: #ff4d4d; }
    .lr-filter-btn.a-warn  { background: rgba(245,158,11,0.12); border-color: rgba(245,158,11,0.4); color: #f59e0b; }
    .lr-filter-btn.a-info  { background: rgba(14,165,233,0.10); border-color: rgba(14,165,233,0.35); color: #0ea5e9; }
    .lr-filter-btn.a-debug { background: rgba(139,149,176,0.10); border-color: rgba(139,149,176,0.3); color: #8b95b0; }
    .lr-select {
      background: ${t.bgInput}; border: 1px solid ${t.borderInput};
      color: ${t.textSecond}; border-radius: 6px; padding: 6px 10px;
      font-family: 'JetBrains Mono', monospace; font-size: 11px;
      cursor: pointer; outline: none; transition: background 0.25s, border 0.25s;
    }
    .lr-select option { background: ${t.bgPanel}; color: ${t.textPrimary}; }
    .live-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
    .live-dot.on { animation: pulse 1s ease-in-out infinite; }
    @keyframes pulse { 0%,100% { opacity:1; transform:scale(1); } 50% { opacity:0.4; transform:scale(0.75); } }
  `;

    return (
        <div style={{ minHeight: "100vh", background: t.bg, color: t.textPrimary, fontFamily: "'JetBrains Mono', monospace", paddingBottom: 40, transition: "background 0.25s, color 0.25s" }}>
            <style>{css}</style>

            {/* ── Header ── */}
            <div style={{
                background: t.bgHeader, borderBottom: `1px solid ${t.border}`,
                padding: "0 24px", display: "flex", alignItems: "center", justifyContent: "space-between", height: 56,
                transition: "background 0.25s, border 0.25s",
                boxShadow: isDark ? "none" : "0 1px 4px rgba(0,0,0,0.06)",
            }}>
                {/* Logo */}
                <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
                    <div style={{
                        width: 30, height: 30, borderRadius: 8,
                        background: isDark ? "linear-gradient(135deg,rgba(77,210,255,0.15),rgba(124,58,237,0.2))" : "linear-gradient(135deg,#e0f2fe,#ede9fe)",
                        border: `1px solid ${isDark ? "rgba(77,210,255,0.3)" : "#c7d2fe"}`,
                        display: "flex", alignItems: "center", justifyContent: "center", fontSize: 15,
                    }}>◈</div>
                    <div>
                        <div style={{ fontSize: 14, fontWeight: 700, color: t.textPrimary, letterSpacing: "0.02em" }}>LogRadar</div>
                        <div style={{ fontSize: 10, color: t.textDim, letterSpacing: "0.1em" }}>SYSTEM LOG MONITOR</div>
                    </div>
                </div>

                {/* Right controls */}
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    {alerts.length > 0 && (
                        <div style={{
                            display: "flex", alignItems: "center", gap: 6,
                            background: "rgba(255,77,77,0.08)", border: "1px solid rgba(255,77,77,0.3)",
                            borderRadius: 6, padding: "4px 10px",
                        }}>
                            <span style={{ fontSize: 12 }}>🔴</span>
                            <span style={{ fontSize: 11, color: "#ff4d4d", fontWeight: 700 }}>{alerts.length} ALERT{alerts.length > 1 ? "S" : ""}</span>
                        </div>
                    )}

                    {/* Live toggle */}
                    <button onClick={() => setLiveMode(p => !p)} style={{
                        background: liveMode ? "rgba(255,77,77,0.10)" : t.bgCard,
                        border: `1px solid ${liveMode ? "rgba(255,77,77,0.4)" : t.border}`,
                        color: liveMode ? "#ff4d4d" : t.textMuted,
                        borderRadius: 6, padding: "6px 14px", cursor: "pointer",
                        fontFamily: "inherit", fontSize: 11, display: "flex", alignItems: "center", gap: 8,
                        fontWeight: 700, letterSpacing: "0.04em", transition: "all 0.2s",
                    }}>
                        <span className={`live-dot ${liveMode ? "on" : ""}`} style={{ background: liveMode ? "#ff4d4d" : t.textDim }} />
                        {liveMode ? "LIVE" : "PAUSED"}
                    </button>

                    {/* Theme toggle */}
                    <ThemeToggle isDark={isDark} onToggle={() => setIsDark(p => !p)} t={t} />

                    <div style={{ fontSize: 11, color: t.textFaint, marginLeft: 4 }}>{new Date().toLocaleTimeString("vi-VN")}</div>
                </div>
            </div>

            {/* ── Body ── */}
            <div style={{ padding: "20px 24px 0" }}>
                {/* Alerts */}
                <AlertBanner alerts={alerts} onDismiss={i => setAlerts(prev => prev.filter((_, idx) => idx !== i))} />

                {/* Stats */}
                <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 12, marginBottom: 20 }}>
                    <StatCard t={t} label="Total Logs" value={stats.total} color={t.textSecond} sublabel="last session" />
                    <StatCard t={t} label="Errors" value={stats.error} color="#ff4d4d" sublabel={`${((stats.error / stats.total) * 100).toFixed(1)}% of total`} />
                    <StatCard t={t} label="Warnings" value={stats.warn} color="#f59e0b" sublabel={`${((stats.warn / stats.total) * 100).toFixed(1)}% of total`} />
                    <div style={{ background: t.bgCard, border: `1px solid ${t.border}`, borderRadius: 10, padding: "14px 18px", transition: "background 0.25s, border 0.25s" }}>
                        <div style={{ fontSize: 11, color: t.textMuted, letterSpacing: "0.08em", textTransform: "uppercase", marginBottom: 6 }}>Error Rate (20m)</div>
                        <MiniChart data={errorHistory} t={t} />
                    </div>
                </div>

                {/* Filter bar */}
                <div style={{
                    background: t.bgPanel, border: `1px solid ${t.border}`,
                    borderRadius: 10, marginBottom: 4,
                    transition: "background 0.25s, border 0.25s",
                    boxShadow: isDark ? "none" : "0 1px 3px rgba(0,0,0,0.04)",
                }}>
                    {/* Row 1: search + level */}
                    <div style={{ padding: "12px 16px", borderBottom: `1px solid ${t.border}`, display: "flex", alignItems: "center", gap: 14, flexWrap: "wrap" }}>
                        <div style={{ position: "relative", flex: "1", minWidth: 200 }}>
                            <span style={{ position: "absolute", left: 10, top: "50%", transform: "translateY(-50%)", color: t.searchIcon, fontSize: 15 }}>⌕</span>
                            <input
                                className="lr-input"
                                value={search} onChange={e => setSearch(e.target.value)}
                                placeholder="Search messages, traceId, service..."
                                style={{
                                    width: "100%", background: t.bgInput, border: `1px solid ${t.borderInput}`,
                                    borderRadius: 7, padding: "7px 12px 7px 30px",
                                    color: t.textPrimary, fontFamily: "inherit", fontSize: 12, outline: "none",
                                    transition: "background 0.25s, border 0.25s, color 0.25s",
                                }}
                            />
                        </div>
                        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                            {LOG_LEVELS.map(l => {
                                const cls = filterLevel === l ? (l === "ALL" ? "a-all" : `a-${l.toLowerCase()}`) : "";
                                return <button key={l} className={`lr-filter-btn ${cls}`} onClick={() => setFilterLevel(l)}>{l}</button>;
                            })}
                        </div>
                    </div>

                    {/* Row 2: env + service + count */}
                    <div style={{ padding: "10px 16px", display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
                        <span style={{ fontSize: 11, color: t.textDim, letterSpacing: "0.08em" }}>ENV:</span>
                        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                            {ENVIRONMENTS.map(e => {
                                const active = filterEnv === e;
                                const cls = active ? (e === "ALL" ? "a-all" : "") : "";
                                const inlineStyle = active && e !== "ALL"
                                    ? { color: envConfig[e]?.color, background: envConfig[e]?.bg, borderColor: `${envConfig[e]?.color}66` }
                                    : {};
                                return <button key={e} className={`lr-filter-btn ${cls}`} style={inlineStyle} onClick={() => setFilterEnv(e)}>{e}</button>;
                            })}
                        </div>
                        <span style={{ fontSize: 11, color: t.textDim, letterSpacing: "0.08em", marginLeft: 8 }}>SERVICE:</span>
                        <select className="lr-select" value={filterService} onChange={e => setFilterService(e.target.value)}>
                            {SERVICES.map(s => <option key={s}>{s}</option>)}
                        </select>
                        <span style={{ marginLeft: "auto", fontSize: 11, color: t.textDim }}>
                            Showing <span style={{ color: t.accentInfo }}>{filtered.length}</span> / {logs.length} logs
                        </span>
                    </div>
                </div>

                {/* Log table */}
                <div style={{
                    background: t.bgPanel, border: `1px solid ${t.border}`,
                    borderRadius: 10, overflow: "hidden",
                    transition: "background 0.25s, border 0.25s",
                    boxShadow: isDark ? "none" : "0 1px 3px rgba(0,0,0,0.04)",
                }}>
                    {/* Column headers */}
                    <div style={{
                        display: "grid", gridTemplateColumns: "90px 62px 88px 140px 1fr",
                        gap: "0 12px", padding: "8px 14px",
                        borderBottom: `1px solid ${t.borderColH}`,
                        background: t.bgColHead,
                    }}>
                        {["TIME", "LEVEL", "ENV", "SERVICE", "MESSAGE"].map(h => (
                            <span key={h} style={{ fontSize: 10, color: t.textFaint, letterSpacing: "0.1em", fontWeight: 700 }}>{h}</span>
                        ))}
                    </div>

                    {/* Rows */}
                    <div ref={listRef} style={{ maxHeight: 480, overflowY: "auto" }}>
                        {filtered.length === 0
                            ? <div style={{ padding: 40, textAlign: "center", color: t.textDim, fontSize: 13 }}>No logs match current filters</div>
                            : filtered.map(log => <LogRow key={log.id} log={log} onClick={setSelectedLog} selected={selectedLog?.id === log.id} t={t} />)
                        }
                    </div>
                </div>

                {/* Detail panel */}
                {selectedLog && <LogDetail log={selectedLog} onClose={() => setSelectedLog(null)} t={t} />}

                {/* Footer */}
                <div style={{ marginTop: 20, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <div style={{ fontSize: 11, color: t.footerText }}>
                        LogRadar v1.0 · Elasticsearch + Logstash + Spring Boot · {ENVIRONMENTS.slice(1).join(" / ")}
                    </div>
                    <div style={{ display: "flex", gap: 16 }}>
                        {Object.entries(envConfig).map(([env, cfg]) => (
                            <span key={env} style={{ fontSize: 11, color: cfg.color, display: "flex", alignItems: "center", gap: 5 }}>
                                <span style={{ width: 6, height: 6, borderRadius: "50%", background: cfg.color, display: "inline-block" }} />
                                {env}: {logs.filter(l => l.env === env).length}
                            </span>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}