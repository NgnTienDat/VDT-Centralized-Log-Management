import { useState } from "react";
import { useAlerts } from "../hooks/useAlerts";
import { useEsIndexFilelds } from "../hooks/useEsIndexFilelds";
import NewAlertRule from "../components/alert/NewAlertRule";
import AlertRuleDetail from "../components/alert/AlertRuleDetail"; // Import component mới tạo

const STATE_BADGE = {
    FIRING: "bg-rose-500/15 text-rose-400 border-rose-500/30",
    OK: "bg-emerald-500/15 text-emerald-400 border-emerald-500/30",
};

export default function AlertMonitor({ isDark }) {
    const [showNewRule, setShowNewRule] = useState(false);
    const [selectedRuleId, setSelectedRuleId] = useState(null); // Quản lý ID rule đang chọn xem chi tiết
    const { 
        fields: esIndexFields, 
        isLoadingFields, 
        isFieldsError, 
        numericFields,
        isLoadingNumericFields,
        isNumericFieldsError } = useEsIndexFilelds();

    const {
        rules,
        isLoadingRules,
        rulesError,
        refetchRules,
        updateRuleAsync,
        isUpdatingRule,
        deleteRuleAsync,
        isDeletingRule,
    } = useAlerts();

    const handleCreated = () => {
        setShowNewRule(false);
        refetchRules();
    };

    const toggleActive = async (e, rule) => {
        e.stopPropagation(); // Ngăn chặn sự kiện click mở Detail panel
        try {
            await updateRuleAsync({ ruleId: rule.ruleId, data: { isActive: !rule.isActive } });
        } catch (e) {
            console.error("[AlertMonitor] Failed to toggle rule:", e);
        }
    };

    const handleDelete = async (e, rule) => {
        e.stopPropagation(); // Ngăn chặn sự kiện click mở Detail panel
        if (!window.confirm(`Xoá rule "${rule.name}"?`)) return;
        try {
            await deleteRuleAsync(rule.ruleId);
            if (selectedRuleId === rule.ruleId) setSelectedRuleId(null);
        } catch (e) {
            console.error("[AlertMonitor] Failed to delete rule:", e);
        }
    };

    return (
        <section className={`mt-6 rounded-2xl border overflow-hidden ${isDark ? "bg-[#0a0f1a] border-white/6" : "bg-white border-slate-200 shadow-sm"}`}>
            <div className={`flex items-center justify-between px-5 py-3 border-b ${isDark ? "border-white/6" : "border-slate-200"}`}>
                <div className="flex items-center gap-3">
                    {/* <div className="w-8 h-8 rounded-lg bg-linear-to-br from-violet-600 to-indigo-500 flex items-center justify-center text-base">🔔</div> */}
                    <div>
                        <h2 className={`text-sm font-semibold tracking-tight ${isDark ? "text-slate-200" : "text-slate-800"}`}>
                            Alert <span className="text-violet-400">Rules</span>
                        </h2>
                        <p className={`text-[10px] ${isDark ? "text-slate-500" : "text-slate-400"}`}>
                            {rules.length} rule{rules.length !== 1 ? "s" : ""}
                        </p>
                    </div>
                </div>
                {/* tesst */}
                {!selectedRuleId && (
                    <button onClick={() => setShowNewRule((p) => !p)} className="text-xs px-3 py-1.5 rounded-lg bg-violet-600 text-white hover:bg-violet-500 transition-colors">
                        {showNewRule ? "Close" : "+ New alert rule"}
                    </button>
                )}
            </div>

            {/* Vùng Render Khối Giao diện Động */}
            {selectedRuleId ? (
                <div className="p-5">
                    <AlertRuleDetail
                        ruleId={selectedRuleId}
                        isDark={isDark}
                        onClose={() => {
                            setSelectedRuleId(null);
                            refetchRules();
                        }}
                    />
                </div>
            ) : (
                <>
                    {showNewRule && (
                        <div className="p-5 border-b border-white/6">
                            <NewAlertRule
                                isDark={isDark}
                                onClose={() => setShowNewRule(false)}
                                onCreated={handleCreated}
                                groupByFields={esIndexFields}
                                isLoadingGroupByFields={isLoadingFields}
                                groupByFieldsError={isFieldsError}
                                numericFields={numericFields}
                                isLoadingNumericFields={isLoadingNumericFields}
                                numericFieldsError={isNumericFieldsError}
                            />
                        </div>
                    )}

                    <div className="p-5 flex flex-col gap-2">
                        {isLoadingRules && <p className="text-xs opacity-50">Đang tải danh sách rule…</p>}
                        {rulesError && <p className="text-xs text-rose-500">Không thể tải danh sách rule.</p>}

                        {!isLoadingRules && rules.length === 0 && (
                            <div className="flex flex-col items-center justify-center h-32 gap-2">
                                <span className="text-3xl opacity-30">📭</span>
                                <p className={`text-xs ${isDark ? "text-slate-600" : "text-slate-400"}`}>
                                    Chưa có rule nào. Bấm "New alert rule" để tạo mới.
                                </p>
                            </div>
                        )}

                        {rules.map((rule) => (
                            <div
                                key={rule.ruleId}
                                onClick={() => setSelectedRuleId(rule.ruleId)} // Click dòng để xem Chi tiết
                                className={`flex items-center justify-between rounded-xl border px-4 py-3 cursor-pointer transition-all ${isDark ? "border-white/6 bg-white/3 hover:bg-white/5" : "border-slate-200 bg-slate-50 hover:bg-slate-100"}`}
                            >
                                <div className="flex items-center gap-3">
                                    <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border ${STATE_BADGE[rule.alertState] ?? "bg-slate-500/15 text-slate-400 border-slate-500/30"}`}>
                                        {rule.alertState ?? "OK"}
                                    </span>
                                    <div>
                                        <p className={`text-sm font-medium ${isDark ? "text-slate-200" : "text-slate-800"}`}>{rule.name}</p>
                                        <p className={`text-[10px] ${isDark ? "text-slate-500" : "text-slate-400"}`}>
                                            Every {rule.intervalMinutes}m · Trigger [{rule.triggerStepId}]
                                        </p>
                                    </div>
                                </div>

                                <div className="flex items-center gap-2">
                                    <button
                                        disabled={isUpdatingRule}
                                        onClick={(e) => toggleActive(e, rule)}
                                        className={`text-[11px] px-2.5 py-1 rounded-lg border font-semibold transition-colors ${rule.isActive ? "border-emerald-500/30 text-emerald-400 bg-emerald-500/5 hover:bg-emerald-500/10" : isDark ? "border-white/10 text-slate-500 hover:bg-white/5" : "border-slate-300 text-slate-500 hover:bg-slate-50"}`}
                                    >
                                        {rule.isActive ? "Active" : "Paused"}
                                    </button>
                                    <button
                                        disabled={isDeletingRule}
                                        onClick={(e) => handleDelete(e, rule)}
                                        className="text-[11px] px-2.5 py-1 rounded-lg text-rose-400 hover:bg-rose-500/10 font-semibold"
                                    >
                                        Delete
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </>
            )}
        </section>
    );
}