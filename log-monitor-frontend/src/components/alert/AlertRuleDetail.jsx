import { useState, useCallback } from "react";
import { useAlerts, useAlertRule } from "../../hooks/useAlerts";
import { useAlertNotifications, usePrependNotification } from "../../hooks/useAlertNotifications";
import NewAlertRule from "./NewAlertRule";
import AlertNotification from "./AlertNotification";

// ── Tab config ────────────────────────────────────────────────────────────────
// Thêm tab mới: push vào đây + thêm 1 dòng render ở TAB CONTENT bên dưới.
// Không cần sửa bất kỳ logic nào khác.
const TABS = [
    { id: "overview", label: "Tổng quan" },
    { id: "notifications", label: "Cảnh báo" },
    // { id: "history", label: "Lịch sử" }, // import AlertHistory rồi uncomment
];

const OPERATOR_LABELS = {
    GREATER_THAN: ">",
    GREATER_THAN_OR_EQUALS: "≥",
    LESS_THAN: "<",
    LESS_THAN_OR_EQUALS: "≤",
    EQUALS: "=",
    NOT_EQUALS: "≠",
};

function formatTimestamp(ts) {
    if (!ts || ts === 0) return "Chưa từng chạy";
    return new Date(ts).toLocaleString("vi-VN", {
        timeZone: "Asia/Ho_Chi_Minh",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
    });
}

export default function AlertRuleDetail({ ruleId, isDark, onClose }) {
    const { data: ruleResult, isLoading, isError } = useAlertRule(ruleId);
    const { data: notifications = [], isLoading: isLoadingNotifications } = useAlertNotifications(ruleId);
    const prependNotification = usePrependNotification();

    const [isEditing, setIsEditing] = useState(false);
    const [activeTab, setActiveTab] = useState("overview");

    // onAlert: lọc theo ruleId và prepend vào cache để UI realtime mà không dùng state cục bộ.
    const onAlert = useCallback(
        (data) => {
            if (data.ruleId !== ruleId) return;
            prependNotification(ruleId, data);
        },
        [prependNotification, ruleId]
    );

    // useAlerts đã tạo WS connection ở AlertMonitor; gọi ở đây chỉ tái dùng
    // mutation/query cache.
    const { refetchRules } = useAlerts(onAlert);

    const rule = ruleResult?.data || ruleResult;

    if (isLoading) {
        return (
            <div className="p-6 text-xs text-slate-400 animate-pulse">
                Đang tải thông tin chi tiết alert rule...
            </div>
        );
    }

    if (isError || !rule) {
        return (
            <div className={`p-4 text-xs font-medium rounded-lg border ${isDark ? "bg-rose-950/20 border-rose-900/50 text-rose-400" : "bg-rose-50 border-rose-200 text-rose-600"}`}>
                ⚠️ Không thể tải thông tin chi tiết cấu hình Rule này hoặc Rule không tồn tại.
            </div>
        );
    }

    if (isEditing) {
        return (
            <NewAlertRule
                isDark={isDark}
                ruleToEdit={rule}
                onClose={() => setIsEditing(false)}
                onCreated={() => {
                    setIsEditing(false);
                    refetchRules();
                }}
            />
        );
    }

    // ── Theme tokens ─────────────────────────────────────────────────────────
    const bgContainer = isDark
        ? "bg-[#0b1019] border-slate-800 text-slate-200"
        : "bg-white border-slate-200 text-slate-800";
    const bgSection = isDark ? "bg-slate-900/40 border-slate-800" : "bg-slate-50 border-slate-200";
    const textMuted = isDark ? "text-slate-400" : "text-slate-500";
    const valueMuted = isDark ? "text-slate-400" : "text-slate-600";
    const textLabel = isDark ? "text-slate-500 font-medium" : "text-slate-400 font-semibold";
    const borderSubtle = isDark ? "border-slate-800" : "border-slate-200";
    const idAccent = isDark ? "text-indigo-400" : "text-indigo-600";
    const chipCls = isDark ? "bg-white/8 text-slate-300" : "bg-slate-100 text-slate-600";
    const codeCls = isDark
        ? "bg-black/30 text-slate-300 border-white/10"
        : "bg-slate-50 text-slate-700 border-slate-200";
    const triggerCardCls = isDark
        ? "border-amber-500/30 bg-amber-500/[0.04]"
        : "border-amber-300 bg-amber-50/70";

    const activeBadgeCls = rule.isActive
        ? isDark
            ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/30"
            : "bg-emerald-50 text-emerald-700 border-emerald-200"
        : isDark
            ? "bg-white/5 text-slate-400 border-white/10"
            : "bg-slate-100 text-slate-500 border-slate-200";

    const stateBadgeCls =
        rule.alertState === "FIRING"
            ? isDark
                ? "bg-rose-500/10 text-rose-400 border-rose-500/30"
                : "bg-rose-50 text-rose-700 border-rose-200"
            : isDark
                ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/30"
                : "bg-emerald-50 text-emerald-700 border-emerald-200";

    // Badge số thông báo chưa đọc trên tab "Thông báo"
    const notifBadge = notifications.length;

    return (
        <div className={`alert-rule-detail-scope rounded-xl p-5 flex flex-col gap-5 transition-all ${bgContainer}`}>
            {/* ── 1. HEADER ── */}
            <div className={`flex items-start justify-between border-b pb-4 ${borderSubtle}`}>
                <div className="flex flex-col gap-1.5">
                    <button
                        onClick={onClose}
                        className={`text-xs w-fit font-medium hover:underline flex items-center gap-1 whitespace-nowrap ${textMuted}`}
                    >
                        ← Danh sách rule
                    </button>
                    <div className="flex items-center gap-2 mt-1 flex-wrap">
                        <h3 className="text-base font-bold tracking-tight">{rule.name}</h3>
                        <span className={`text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded border ${activeBadgeCls}`}>
                            {rule.isActive ? "Đang chạy" : "Tạm dừng"}
                        </span>
                        <span className={`text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded border ${stateBadgeCls}`}>
                            {rule.alertState || "OK"}
                        </span>
                    </div>
                    <p className={`text-xs font-mono ${textMuted}`}>
                        ID: <span className={idAccent}>{rule.ruleId}</span>
                    </p>
                </div>

                <button
                    onClick={() => setIsEditing(true)}
                    className="text-xs px-3.5 py-1.5 rounded-md bg-indigo-600 text-white font-medium hover:bg-indigo-500 shadow-sm transition-colors whitespace-nowrap"
                >
                    Chỉnh sửa (Edit)
                </button>
            </div>

            {/* ── TAB BAR ── */}
            {/* Thêm tab: push vào TABS ở trên, thêm case ở TAB CONTENT bên dưới */}
            <div className={`flex items-center flex-wrap gap-1 border-b -mt-2 ${borderSubtle}`}>
                {TABS.map((tab) => {
                    const isActive = activeTab === tab.id;
                    const badge = tab.id === "notifications" ? notifBadge : 0;
                    return (
                        <button
                            key={tab.id}
                            onClick={() => setActiveTab(tab.id)}
                            className={[
                                "flex items-center gap-1.5 text-xs px-4 py-2.5 font-medium whitespace-nowrap",
                                "border-b-2 -mb-px transition-colors",
                                isActive
                                    ? `border-indigo-500 ${isDark ? "text-indigo-400" : "text-indigo-600"}`
                                    : `border-transparent ${isDark ? "text-slate-500 hover:text-slate-300" : "text-slate-400 hover:text-slate-700"}`,
                            ].join(" ")}
                        >
                            {tab.label}
                            {badge > 0 && (
                                <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded-full ${isDark ? "bg-rose-500/20 text-rose-400" : "bg-rose-100 text-rose-600"}`}>
                                    {badge}
                                </span>
                            )}
                        </button>
                    );
                })}
            </div>

            {/* ── TAB CONTENT ── */}

            {/* Tab: Tổng quan */}
            {activeTab === "overview" && (
                <>
                    {/* 2. OVERVIEW METRICS GRID */}
                    <div className={`grid grid-cols-2 md:grid-cols-3 gap-y-4 gap-x-6 p-4 rounded-lg border ${bgSection}`}>
                        <div>
                            <span className={`text-[10px] uppercase tracking-wider block mb-0.5 ${textLabel}`}>Tần suất quét (Interval)</span>
                            <span className={`text-xs font-semibold ${idAccent}`}>{rule.intervalMinutes} phút / lần</span>
                        </div>
                        <div>
                            <span className={`text-[10px] uppercase tracking-wider block mb-0.5 ${textLabel}`}>Cooldown thông báo</span>
                            <span className="text-xs font-medium">{rule.repeatIntervalMinutes} phút</span>
                        </div>
                        <div>
                            <span className={`text-[10px] uppercase tracking-wider block mb-0.5 ${textLabel}`}>Khối kích hoạt chính</span>
                            <span className={`text-xs font-mono font-bold ${isDark ? "text-amber-400" : "text-amber-600"}`}>
                                Step [{rule.triggerStepId}]
                            </span>
                        </div>
                        <div className={`col-span-2 md:col-span-1 border-t pt-2 md:border-t-0 md:pt-0 ${borderSubtle}`}>
                            <span className={`text-[10px] uppercase tracking-wider block mb-0.5 ${textLabel}`}>Lần kiểm tra cuối cùng</span>
                            <span className={`text-xs font-medium ${valueMuted}`}>{formatTimestamp(rule.lastRunTime)}</span>
                        </div>
                        <div className={`border-t pt-2 md:border-t-0 md:pt-0 ${borderSubtle}`}>
                            <span className={`text-[10px] uppercase tracking-wider block mb-0.5 ${textLabel}`}>Lần bắn thông báo cuối</span>
                            <span className={`text-xs font-medium ${valueMuted}`}>{formatTimestamp(rule.lastNotifiedTime)}</span>
                        </div>
                        <div className={`border-t pt-2 md:border-t-0 md:pt-0 ${borderSubtle}`}>
                            <span className={`text-[10px] uppercase tracking-wider block mb-0.5 ${textLabel}`}>Nhóm vi phạm gần nhất</span>
                            <div className="mt-0.5">
                                {rule.lastNotifiedBreachedGroups?.length > 0 ? (
                                    rule.lastNotifiedBreachedGroups.map((g) => (
                                        <span key={g} className={`inline-block px-1 py-0.5 rounded mr-1 text-[11px] font-mono ${chipCls}`}>
                                            {g}
                                        </span>
                                    ))
                                ) : (
                                    <span className={`italic text-[11px] ${textMuted}`}>Không có</span>
                                )}
                            </div>
                        </div>
                    </div>

                    {/* 3. PIPELINE */}
                    <div className="flex flex-col gap-3">
                        <h4 className={`text-xs font-bold uppercase tracking-wider ${textMuted}`}>
                            Luồng xử lý dữ liệu (Pipeline)
                        </h4>
                        <div className="flex flex-col gap-2">
                            {rule.pipelineSteps?.map((step) => {
                                const isTrigger = rule.triggerStepId === step.id;
                                return (
                                    <div
                                        key={step.id}
                                        className={`rounded-lg border p-3.5 flex flex-col gap-2 ${isTrigger ? triggerCardCls : `${borderSubtle} bg-transparent`}`}
                                    >
                                        <div className="flex items-center justify-between flex-wrap gap-2">
                                            <div className="flex items-center gap-2">
                                                <span className={`text-xs font-mono font-bold px-1.5 py-0.5 rounded ${chipCls}`}>
                                                    [{step.id}]
                                                </span>
                                                <span className="text-xs font-semibold">
                                                    {step.type === "FETCH_ES_DATA" && "Truy vấn Elasticsearch Data"}
                                                    {step.type === "MATH" && "Biểu thức Toán học"}
                                                    {step.type === "EVALUATE_THRESHOLD" && "Đánh giá Điều kiện Ngưỡng"}
                                                </span>
                                            </div>
                                            {isTrigger && (
                                                <span className={`text-[10px] font-medium flex items-center gap-1 px-2 py-0.5 rounded ${isDark ? "text-amber-400 bg-amber-500/10" : "text-amber-700 bg-amber-100"}`}>
                                                    🎯 Target Trigger
                                                </span>
                                            )}
                                        </div>

                                        {step.type === "FETCH_ES_DATA" && (
                                            <div className={`text-xs pl-2 border-l-2 flex flex-col gap-2 ${isDark ? "border-indigo-500/50" : "border-indigo-300"}`}>
                                                <div className={`font-mono p-2 rounded text-[11px] overflow-x-auto border whitespace-pre-wrap ${codeCls}`}>
                                                    {step.params?.query || "(Không có query)"}
                                                </div>
                                                <div className={`grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-1 text-[11px] ${textMuted}`}>
                                                    <p>• Lookback: <span className={`font-medium ${valueMuted}`}>{step.params?.lookBackMinutes} phút trước</span></p>
                                                    <p>• Group By: <span className={`font-mono font-medium ${valueMuted}`}>{step.params?.groupBy?.length > 0 ? step.params.groupBy.join(", ") : "None"}</span></p>
                                                    <p>• Metric: <span className={`font-medium ${valueMuted}`}>{step.params?.metricType}{step.params?.metricField ? ` (${step.params.metricField})` : ""}</span></p>
                                                    <p>• Index: <span className={`font-mono ${textMuted}`}>{step.params?.index}</span></p>
                                                </div>
                                            </div>
                                        )}

                                        {step.type === "MATH" && (
                                            <div className={`text-xs pl-2 border-l-2 flex flex-col gap-1.5 ${isDark ? "border-sky-500/50" : "border-sky-300"}`}>
                                                <p className={`text-[11px] ${textMuted}`}>
                                                    Nguồn đầu vào:{" "}
                                                    {(Array.isArray(step.params?.input) ? step.params.input : [step.params?.input])
                                                        .filter(Boolean)
                                                        .map((i) => (
                                                            <span key={i} className={`font-mono font-bold mx-0.5 ${idAccent}`}>[{i}]</span>
                                                        ))}
                                                </p>
                                                <div className={`font-mono p-2 rounded text-[11px] border ${codeCls}`}>
                                                    {step.params?.expression}
                                                </div>
                                            </div>
                                        )}

                                        {step.type === "EVALUATE_THRESHOLD" && (
                                            <div className={`text-xs pl-2 border-l-2 py-1 flex items-center flex-wrap gap-1.5 font-medium ${isDark ? "border-rose-500/50" : "border-rose-300"}`}>
                                                <span className={textMuted}>Kiểm tra kết quả của</span>
                                                <span className={`font-mono font-bold ${idAccent}`}>[{step.params?.input}]</span>
                                                <span className={`font-bold px-1 ${isDark ? "text-rose-400" : "text-rose-600"}`}>
                                                    {OPERATOR_LABELS[step.params?.operator] ?? step.params?.operator}
                                                </span>
                                                <span className={`font-mono font-bold px-2 py-0.5 rounded text-[11px] ${chipCls}`}>
                                                    {step.params?.value}
                                                </span>
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    </div>

                    {/* 4. NOTIFICATION TEMPLATE */}
                    <div className={`border-t pt-4 flex flex-col gap-2.5 ${borderSubtle}`}>
                        <h4 className={`text-xs font-bold uppercase tracking-wider ${textMuted}`}>
                            Mẫu thông báo khi kích hoạt (Notification Template)
                        </h4>
                        <div className="flex flex-col gap-2 text-xs">
                            <div>
                                <span className={`text-[10px] block mb-1 ${textLabel}`}>Tiêu đề Alert</span>
                                <div className={`p-2 rounded font-medium border ${bgSection}`}>
                                    {rule.notificationTemplate?.title}
                                </div>
                            </div>
                            <div>
                                <span className={`text-[10px] block mb-1 ${textLabel}`}>Nội dung tin nhắn</span>
                                <div className={`p-2 rounded font-mono whitespace-pre-wrap border text-[11px] ${bgSection} ${textMuted}`}>
                                    {rule.notificationTemplate?.message || "(Trống)"}
                                </div>
                            </div>
                        </div>
                    </div>
                </>
            )}

            {/* Tab: Thông báo */}
            {activeTab === "notifications" && (
                isLoadingNotifications ? (
                    <div className="p-4 text-xs text-slate-400 animate-pulse">
                        Đang tải lịch sử thông báo...
                    </div>
                ) : (
                    <AlertNotification notifications={notifications} isDark={isDark} />
                )
            )}

            {/* Thêm tab mới ở đây, ví dụ:
            {activeTab === "history" && (
                <AlertHistory ruleId={ruleId} isDark={isDark} />
            )} */}
        </div>
    );
}