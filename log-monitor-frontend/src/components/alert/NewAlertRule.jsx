import { useState, useRef, useCallback, useMemo } from "react";
import { useAlerts } from "../../hooks/useAlerts";

// ── Hidden from UI per UX requirements, backend automatically attaches during payload construction ────────────
const DEFAULT_INDEX = "sys-logs-*";
const DEFAULT_TIME_FIELD = "@timestamp";

const GROUP_BY_OPTIONS = ["service", "status", "env", "host"];

// Matches backend ThresholdOperator enum
const THRESHOLD_OPERATORS = [
    { value: "GREATER_THAN", label: "> Greater than" },
    { value: "GREATER_THAN_OR_EQUALS", label: "≥ Greater than or equal to" },
    { value: "LESS_THAN", label: "< Less than" },
    { value: "LESS_THAN_OR_EQUALS", label: "≤ Less than or equal to" },
    { value: "EQUALS", label: "= Equals" },
    { value: "NOT_EQUALS", label: "≠ Not equal to" },
];

// A, B, ..., Z, AA, AB... (Grafana / Excel column style)
function indexToLabel(index) {
    let label = "";
    let n = index;
    do {
        label = String.fromCharCode(65 + (n % 26)) + label;
        n = Math.floor(n / 26) - 1;
    } while (n >= 0);
    return label;
}

const createFetchStep = (id) => ({
    id,
    type: "FETCH_ES_DATA",
    query: "",
    lookBackMinutes: 5,
    groupBy: [],
    metricType: "COUNT", // COUNT | SUM | AVG | MIN | MAX
    metricField: "",
});
const createMathStep = (id) => ({ id, type: "MATH", inputs: [], expression: "" });
const createThresholdStep = (id) => ({ id, type: "EVALUATE_THRESHOLD", inputStepId: "", operator: "GREATER_THAN", value: "" });

export default function NewAlertRule({
    isDark,
    onClose,
    onCreated,
    ruleToEdit,
    groupByFields = [],
    isLoadingGroupByFields = false,
    groupByFieldsError = false,
    numericFields = [],
    isLoadingNumericFields = false,
    numericFieldsError = false }) {

    const { createRuleAsync, updateRuleAsync, isCreatingRule, createRuleError } = useAlerts();

    const idCounterRef = useRef(1);

    // Initialize flexible State based on whether creating new or editing existing
    const [name, setName] = useState(ruleToEdit ? ruleToEdit.name : "");

    // Split intervalMinutes back into initial Value and Unit
    const [intervalValue, setIntervalValue] = useState(() => {
        if (!ruleToEdit) return 1;
        const mins = ruleToEdit.intervalMinutes;
        return mins % 60 === 0 && mins >= 60 ? mins / 60 : mins;
    });
    const [intervalUnit, setIntervalUnit] = useState(() => {
        if (!ruleToEdit) return "minutes";
        return ruleToEdit.intervalMinutes % 60 === 0 && ruleToEdit.intervalMinutes >= 60 ? "hours" : "minutes";
    });

    // Convert pipeline structure from Backend params to UI State structure
    const [steps, setSteps] = useState(() => {
        if (!ruleToEdit || !ruleToEdit.pipelineSteps) {
            return [createFetchStep("A")];
        }
        // Sync idCounterRef so adding a new Step won't duplicate an old ID
        idCounterRef.current = ruleToEdit.pipelineSteps.length;

        return ruleToEdit.pipelineSteps.map((s) => {
            if (s.type === "FETCH_ES_DATA") {
                return {
                    id: s.id,
                    type: "FETCH_ES_DATA",
                    query: s.params?.query || "",
                    lookBackMinutes: s.params?.lookBackMinutes || 5,
                    groupBy: s.params?.groupBy || [],
                    metricType: s.params?.metricType || "COUNT",
                    metricField: s.params?.metricField || "",
                };
            }
            if (s.type === "MATH") {
                return {
                    id: s.id,
                    type: "MATH",
                    inputs: s.params?.input || [],
                    expression: s.params?.expression || "",
                };
            }
            if (s.type === "EVALUATE_THRESHOLD") {
                return {
                    id: s.id,
                    type: "EVALUATE_THRESHOLD",
                    inputStepId: s.params?.input || "",
                    operator: s.params?.operator || "GREATER_THAN",
                    value: s.params?.value !== undefined ? s.params.value : "",
                };
            }
            return s;
        });
    });

    const [triggerStepId, setTriggerStepId] = useState(ruleToEdit ? ruleToEdit.triggerStepId : "");
    const [alertTitle, setAlertTitle] = useState(ruleToEdit ? ruleToEdit.notificationTemplate?.title : "🚨 Error Rate Too High on {breachedGroups}");
    const [alertMessage, setAlertMessage] = useState(ruleToEdit ? ruleToEdit.notificationTemplate?.message : "");
    const [repeatIntervalMinutes, setRepeatIntervalMinutes] = useState(ruleToEdit ? ruleToEdit.repeatIntervalMinutes : 15);
    const [showAdvanced, setShowAdvanced] = useState(ruleToEdit ? ruleToEdit.repeatIntervalMinutes !== 15 : false);

    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    const [formError, setFormError] = useState("");
    const nextId = useCallback(() => {
        const label = indexToLabel(idCounterRef.current);
        idCounterRef.current += 1;
        return label;
    }, []);

    const updateStep = useCallback((id, patch) => {
        setSteps((prev) => prev.map((s) => (s.id === id ? { ...s, ...patch } : s)));
    }, []);

    const removeStep = useCallback((id) => {
        setSteps((prev) =>
            prev
                .filter((s) => s.id !== id)
                .map((s) => {
                    if (s.type === "MATH" && s.inputs.includes(id)) {
                        return { ...s, inputs: s.inputs.filter((i) => i !== id) };
                    }
                    if (s.type === "EVALUATE_THRESHOLD" && s.inputStepId === id) {
                        return { ...s, inputStepId: "" };
                    }
                    return s;
                })
        );
        setTriggerStepId((prev) => (prev === id ? "" : prev));
    }, []);

    const addQueryStep = useCallback(() => {
        setSteps((prev) => {
            const id = nextId();
            const reversedIdx = [...prev].reverse().findIndex((s) => s.type === "FETCH_ES_DATA");
            const insertAt = reversedIdx === -1 ? 0 : prev.length - reversedIdx;
            const next = [...prev];
            next.splice(insertAt, 0, createFetchStep(id));
            return next;
        });
    }, [nextId]);

    const addExpressionStep = useCallback((type) => {
        setSteps((prev) => {
            const id = nextId();
            const step = type === "MATH" ? createMathStep(id) : createThresholdStep(id);
            return [...prev, step];
        });
    }, [nextId]);

    const numericStepsBefore = useCallback(
        (index) => steps.slice(0, index).filter((s) => s.type === "FETCH_ES_DATA" || s.type === "MATH"),
        [steps]
    );

    const fetchStepCount = useMemo(() => steps.filter((s) => s.type === "FETCH_ES_DATA").length, [steps]);

    const groupByOptions = useMemo(() => {
        if (Array.isArray(groupByFields) && groupByFields.length > 0) {
            return groupByFields;
        }
        return GROUP_BY_OPTIONS;
    }, [groupByFields]);

    // Split steps based on UI block categories
    const querySteps = useMemo(() => steps.filter((s) => s.type === "FETCH_ES_DATA"), [steps]);
    const expressionSteps = useMemo(() => steps.filter((s) => s.type === "MATH" || s.type === "EVALUATE_THRESHOLD"), [steps]);

    function validate() {
        if (!name.trim()) return "Please enter a Rule Name.";
        if (fetchStepCount === 0) return "At least 1 Query Logs Data block is required.";
        for (const s of steps) {
            if (s.type === "FETCH_ES_DATA") {
                if (s.metricType !== "COUNT" && !s.metricField.trim()) {
                    return `Block [${s.id}] has metric type "${s.metricType}" selected but requires a Metric Field.`;
                }
            }
            if (s.type === "MATH") {
                if (s.inputs.length === 0) return `Block [${s.id}] requires an Input Source.`;
                if (!s.expression.trim()) return `Block [${s.id}] requires an Expression.`;
            }
            if (s.type === "EVALUATE_THRESHOLD") {
                if (!s.inputStepId) return `Block [${s.id}] requires an Input Source.`;
                if (s.value === "" || s.value === null) return `Block [${s.id}] requires a threshold value.`;
            }
        }
        if (!triggerStepId) return 'Please select one block as a trigger ("🎯 Use as Trigger").';
        if (!alertTitle.trim()) return "Please enter an Alert Title.";
        return "";
    }

    function buildPayload() {
        const intervalMinutes = intervalUnit === "hours" ? Number(intervalValue) * 60 : Number(intervalValue);

        const pipelineSteps = steps.map((s) => {
            if (s.type === "FETCH_ES_DATA") {
                return {
                    id: s.id,
                    type: "FETCH_ES_DATA",
                    params: {
                        query: s.query.trim(),
                        lookBackMinutes: Number(s.lookBackMinutes) || 0,
                        groupBy: s.groupBy,
                        metricType: s.metricType,
                        metricField: s.metricType === "COUNT" ? null : s.metricField,
                        index: DEFAULT_INDEX,
                        timeField: DEFAULT_TIME_FIELD,
                    },
                };
            }
            if (s.type === "MATH") {
                return { id: s.id, type: "MATH", params: { input: s.inputs, expression: s.expression } };
            }
            return {
                id: s.id,
                type: "EVALUATE_THRESHOLD",
                params: { input: s.inputStepId, operator: s.operator, value: Number(s.value) },
            };
        });

        return {
            name,
            intervalMinutes,
            isActive: true,
            triggerStepId,
            pipelineSteps,
            repeatIntervalMinutes: Number(repeatIntervalMinutes) || 0,
            notificationTemplate: { title: alertTitle, message: alertMessage },
        };
    }

    async function handleSave() {
        const err = validate();
        if (err) {
            setFormError(err);
            return;
        }
        setFormError("");
        try {
            const payload = buildPayload();

            if (ruleToEdit) {
                // EDIT Mode -> call PATCH via updateRuleAsync
                await updateRuleAsync({
                    ruleId: ruleToEdit.ruleId,
                    data: payload
                });
                onCreated?.();
            } else {
                // CREATE Mode -> call POST
                const saved = await createRuleAsync(payload);
                onCreated?.(saved);
            }
        } catch (e) {
            console.error("Error handling alert rule save:", e);
        }
    }

    const inputCls = isDark
        ? "bg-white/5 border-white/10 text-slate-200 placeholder:text-slate-600 focus:border-violet-500 focus:ring-1 focus:ring-violet-500 focus:outline-none"
        : "bg-white border-slate-400 text-slate-900 placeholder:text-slate-500 focus:border-violet-600 focus:ring-1 focus:ring-violet-600 focus:outline-none";
    const labelCls = `text-xs font-bold mb-1 block ${isDark ? "text-slate-400" : "text-slate-700"}`;
    const cardCls = isDark ? "bg-[#0a0f1a] border-white/10 text-slate-200" : "bg-white border-slate-350 text-slate-900 shadow-md";

    return (
        <div className={`alert-rule-detail-scope rounded-2xl p-5 flex flex-col gap-6 ${cardCls}`}>
            <div className="flex items-center justify-between">
                <h3 className={`text-sm font-bold ${isDark ? "text-slate-200" : "text-slate-900"}`}>Create New Rule</h3>
                <button onClick={onClose} className={`text-xs px-2 py-1 rounded-lg ${isDark ? "hover:bg-white/10 text-slate-400" : "hover:bg-slate-100 text-slate-500"}`}>✕</button>
            </div>

            {/* Section 1 */}
            <section className="flex flex-col gap-3">
                <h4 className={`text-xs font-bold uppercase tracking-wide ${isDark ? "text-slate-400" : "text-slate-700"}`}>1. Rule Information</h4>
                <div>
                    <label className={labelCls}>Rule Name</label>
                    <input className={`w-full rounded-lg border px-3 py-2 text-sm ${inputCls}`} placeholder="e.g., Error Spike Alert" value={name} onChange={(e) => setName(e.target.value)} />
                </div>
                <div className="flex items-end gap-2">
                    <div>
                        <label className={labelCls}>Evaluate every</label>
                        <input type="number" min={1} className={`w-24 rounded-lg border px-3 py-2 text-sm ${inputCls}`} value={intervalValue} onChange={(e) => setIntervalValue(e.target.value)} />
                    </div>
                    <select className={`rounded-lg border px-3 py-2 text-sm ${inputCls}`} value={intervalUnit} onChange={(e) => setIntervalUnit(e.target.value)}>
                        <option value="minutes">minutes</option>
                        <option value="hours">hours</option>
                    </select>
                </div>
            </section>

            {/* Section 2 */}
            <section className="flex flex-col gap-5">
                <h4 className={`text-xs font-bold uppercase tracking-wide ${isDark ? "text-slate-400" : "text-slate-700"}`}>2. Define Query and Alert Conditions</h4>

                {/* Query block section */}
                <div className="flex flex-col gap-3">
                    {querySteps.map((step) => {
                        const originalIndex = steps.findIndex((s) => s.id === step.id);
                        return (
                            <PipelineStepBlock
                                key={step.id}
                                step={step}
                                isDark={isDark}
                                inputOptions={numericStepsBefore(originalIndex)}
                                triggerStepId={triggerStepId}
                                onChange={(patch) => updateStep(step.id, patch)}
                                onRemove={() => removeStep(step.id)}
                                onSetTrigger={() => setTriggerStepId(step.id)}
                                canRemove={!(step.type === "FETCH_ES_DATA" && fetchStepCount === 1)}
                                groupByOptions={groupByOptions}
                                isLoadingGroupByFields={isLoadingGroupByFields}
                                groupByFieldsError={groupByFieldsError}
                                numericFields={numericFields}
                                isLoadingNumericFields={isLoadingNumericFields}
                                numericFieldsError={numericFieldsError}
                            />
                        );
                    })}
                    <div>
                        <button type="button" onClick={addQueryStep} className={`text-xs px-3 py-1.5 rounded-lg border font-semibold ${isDark ? "border-white/10 hover:bg-white/5 text-slate-300" : "border-slate-400 hover:bg-slate-50 text-slate-700"}`}>
                            + Add query
                        </button>
                    </div>
                </div>

                {/* Expression block section */}
                <div className={`flex flex-col gap-3 border-t pt-4 ${isDark ? "border-white/5" : "border-slate-200"}`}>
                    <h5 className={`text-xs font-bold ${isDark ? "text-slate-300" : "text-slate-700"}`}>Expressions</h5>

                    {expressionSteps.length > 0 && (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            {expressionSteps.map((step) => {
                                const originalIndex = steps.findIndex((s) => s.id === step.id);
                                return (
                                    <PipelineStepBlock
                                        key={step.id}
                                        step={step}
                                        isDark={isDark}
                                        inputOptions={numericStepsBefore(originalIndex)}
                                        triggerStepId={triggerStepId}
                                        onChange={(patch) => updateStep(step.id, patch)}
                                        onRemove={() => removeStep(step.id)}
                                        onSetTrigger={() => setTriggerStepId(step.id)}
                                        canRemove={true}
                                        groupByOptions={groupByOptions}
                                        isLoadingGroupByFields={isLoadingGroupByFields}
                                        groupByFieldsError={groupByFieldsError}
                                        numericFields={numericFields}
                                        isLoadingNumericFields={isLoadingNumericFields}
                                        numericFieldsError={numericFieldsError}
                                    />
                                );
                            })}
                        </div>
                    )}

                    {/* Grafana-style Dropdown Selection */}
                    <div className="relative self-start">
                        <button
                            type="button"
                            onClick={() => setIsDropdownOpen((prev) => !prev)}
                            className={`text-xs px-3 py-1.5 rounded-lg border font-semibold flex items-center gap-1 ${isDark ? "border-white/10 hover:bg-white/5 text-slate-300" : "border-slate-400 hover:bg-slate-50 text-slate-700"}`}
                        >
                            + Add expression <span className="text-[10px] opacity-60">▼</span>
                        </button>
                        {isDropdownOpen && (
                            <>
                                <div className="fixed inset-0 z-10" onClick={() => setIsDropdownOpen(false)} />
                                <div className={`absolute left-0 mt-1 w-40 rounded-lg border shadow-lg z-20 flex flex-col p-1 ${isDark ? "bg-[#111625] border-white/10 text-slate-200" : "bg-white border-slate-300 text-slate-900"}`}>
                                    <button
                                        type="button"
                                        onClick={() => {
                                            addExpressionStep("MATH");
                                            setIsDropdownOpen(false);
                                        }}
                                        className={`text-left text-xs px-3 py-2 rounded-md font-medium ${isDark ? "hover:bg-white/5 text-slate-300" : "hover:bg-slate-100 text-slate-700"}`}
                                    >
                                        Math
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => {
                                            addExpressionStep("THRESHOLD");
                                            setIsDropdownOpen(false);
                                        }}
                                        className={`text-left text-xs px-3 py-2 rounded-md font-medium ${isDark ? "hover:bg-white/5 text-slate-300" : "hover:bg-slate-100 text-slate-700"}`}
                                    >
                                        Threshold
                                    </button>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            </section>

            {/* Section 3 */}
            <section className="flex flex-col gap-3">
                <h4 className={`text-xs font-bold uppercase tracking-wide ${isDark ? "text-slate-400" : "text-slate-700"}`}>3. Notification Template</h4>
                <div>
                    <label className={labelCls}>Alert Title</label>
                    <input className={`w-full rounded-lg border px-3 py-2 text-sm ${inputCls}`} value={alertTitle} onChange={(e) => setAlertTitle(e.target.value)} />
                </div>
                <div>
                    <label className={labelCls}>Alert Message</label>
                    <textarea rows={3} className={`w-full rounded-lg border px-3 py-2 text-sm ${inputCls}`} placeholder="System error rate exceeded {thresholdValue}%. Actual value: {actualValues}" value={alertMessage} onChange={(e) => setAlertMessage(e.target.value)} />
                </div>
                <button type="button" onClick={() => setShowAdvanced((p) => !p)} className={`text-xs font-semibold self-start ${isDark ? "text-violet-400" : "text-violet-650"}`}>
                    {showAdvanced ? "▲ Advanced Settings" : "▼ Advanced Settings"}
                </button>
                {showAdvanced && (
                    <div>
                        <label className={labelCls}>Repeat Interval / Cooldown (minutes)</label>
                        <input type="number" min={1} className={`w-32 rounded-lg border px-3 py-2 text-sm ${inputCls}`} value={repeatIntervalMinutes} onChange={(e) => setRepeatIntervalMinutes(e.target.value)} />
                    </div>
                )}
            </section>

            {(formError || createRuleError) && (
                <p className="text-xs text-rose-600 font-semibold">{formError || createRuleError?.message || "An error occurred, please try again."}</p>
            )}

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-200 dark:border-white/10">
                <button type="button" onClick={onClose} className={`text-xs px-4 py-2 rounded-lg font-semibold ${isDark ? "hover:bg-white/10 text-slate-400" : "hover:bg-slate-100 text-slate-655"}`}>Cancel</button>
                <button type="button" disabled={isCreatingRule} onClick={handleSave} className="text-xs px-4 py-2 rounded-lg bg-violet-600 text-white hover:bg-violet-500 disabled:opacity-50 font-semibold">
                    {isCreatingRule ? "Saving..." : ruleToEdit ? "Save Changes" : "Save"}
                </button>
            </div>
        </div>
    );
}

function PipelineStepBlock({
    step,
    isDark,
    inputOptions,
    triggerStepId,
    onChange,
    onRemove,
    onSetTrigger,
    canRemove,
    groupByOptions = GROUP_BY_OPTIONS,
    isLoadingGroupByFields = false,
    groupByFieldsError = false,
    numericFields = [],
    isLoadingNumericFields = false,
    numericFieldsError = false,
}) {
    const blockCls = isDark ? "bg-white/3 border-white/8" : "bg-slate-50 border-slate-300 shadow-xs";
    const inputCls = isDark
        ? "bg-white/5 border-white/10 text-slate-200 placeholder:text-slate-600 focus:border-violet-500 focus:ring-1 focus:ring-violet-500 focus:outline-none"
        : "bg-white border-slate-400 text-slate-900 placeholder:text-slate-500 focus:border-violet-600 focus:ring-1 focus:ring-violet-600 focus:outline-none";
    const labelCls = `text-[11px] font-bold mb-1 block ${isDark ? "text-slate-400" : "text-slate-700"}`;

    const headerLabel =
        step.type === "FETCH_ES_DATA" ? "Query Logs Data (Elasticsearch)" : step.type === "MATH" ? "Math Expression" : "Evaluate Threshold";
    const canBeTrigger = step.type === "MATH" || step.type === "EVALUATE_THRESHOLD";

    return (
        <div className={`rounded-xl border p-4 flex flex-col gap-3 ${blockCls}`}>
            <div className="flex items-center justify-between">
                <span className={`text-xs font-bold ${isDark ? "text-slate-200" : "text-slate-900"}`}>
                    <span className={isDark ? "text-violet-400" : "text-violet-600"}>[{step.id}]</span> {headerLabel}
                </span>
                <div className="flex items-center gap-3">
                    {canBeTrigger && (
                        <label className={`flex items-center gap-1.5 text-[11px] font-semibold cursor-pointer ${isDark ? "text-slate-300" : "text-slate-700"}`}>
                            <input type="radio" name="triggerStepId" checked={triggerStepId === step.id} onChange={onSetTrigger} className="accent-violet-600" />
                            Use as Trigger
                        </label>
                    )}
                    {canRemove && (
                        <button type="button" onClick={onRemove} className={`text-[11px] font-bold ${isDark ? "text-slate-400 hover:text-slate-200" : "text-slate-500 hover:text-slate-900"}`}>✕ Remove</button>
                    )}
                </div>
            </div>

            {step.type === "FETCH_ES_DATA" && (
                <>
                    <div>
                        <label className={labelCls}>Query (Lucene, optional)</label>
                        <textarea rows={2} className={`w-full rounded-lg border px-3 py-2 text-sm font-mono ${inputCls}`} placeholder="Leave empty to match all logs" value={step.query} onChange={(e) => onChange({ query: e.target.value })} />
                    </div>
                    <div className="flex items-end gap-2">
                        <div>
                            <label className={labelCls}>Lookback</label>
                            <input type="number" min={1} className={`w-20 rounded-lg border px-3 py-2 text-sm ${inputCls}`} value={step.lookBackMinutes} onChange={(e) => onChange({ lookBackMinutes: e.target.value })} />
                        </div>
                        <span className={`text-xs font-semibold ${isDark ? "text-slate-400" : "text-slate-600"} pb-2.5`}>minutes ago</span>
                    </div>
                    <div>
                        <label className={labelCls}>Group By</label>
                        <div className="flex flex-wrap gap-1.5">
                            {isLoadingGroupByFields && <span className={`text-[11px] ${isDark ? "text-slate-500" : "text-slate-400"}`}>Loading fields...</span>}
                            {groupByFieldsError && <span className="text-[11px] text-rose-500">Cannot load fields.</span>}
                            {groupByOptions.map((field) => {
                                const active = step.groupBy.includes(field);
                                return (
                                    <button key={field} type="button"
                                        onClick={() => onChange({ groupBy: active ? step.groupBy.filter((f) => f !== field) : [...step.groupBy, field] })}
                                        className={`text-[11px] px-2.5 py-1 rounded-full border font-semibold transition-colors ${active ? "bg-violet-600 border-violet-600 text-white" : isDark ? "border-white/10 text-slate-400 hover:bg-white/5" : "border-slate-400 text-slate-700 hover:bg-slate-100"}`}>
                                        {field}
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                    <div className="flex items-end gap-2">
                        <div>
                            <label className={labelCls}>Metric Type</label>
                            <select
                                className={`rounded-lg border px-3 py-2 text-sm ${inputCls}`}
                                value={step.metricType}
                                onChange={(e) => onChange({ metricType: e.target.value })}
                            >
                                <option value="COUNT">COUNT</option>
                                <option value="SUM">SUM</option>
                                <option value="AVG">AVG</option>
                                <option value="MIN">MIN</option>
                                <option value="MAX">MAX</option>
                            </select>
                        </div>
                        {step.metricType !== "COUNT" && (
                            <div>
                                <label className={labelCls}>Metric Field</label>
                                <select
                                    className={`w-44 rounded-lg border px-3 py-2 text-sm ${inputCls}`}
                                    value={step.metricField}
                                    onChange={(e) => onChange({ metricField: e.target.value })}
                                    disabled={isLoadingNumericFields || numericFieldsError}
                                >
                                    {isLoadingNumericFields ? (
                                        <option value="">Loading fields...</option>
                                    ) : numericFieldsError ? (
                                        <option value="">Error loading fields</option>
                                    ) : numericFields.length === 0 ? (
                                        // Trường hợp kết quả trả về [] rỗng, hiển thị none
                                        <option value="">none</option>
                                    ) : (
                                        <>
                                            <option value="">— Select field —</option>
                                            {numericFields.map((field) => (
                                                <option key={field} value={field}>
                                                    {field}
                                                </option>
                                            ))}
                                        </>
                                    )}
                                </select>
                            </div>
                        )}
                    </div>
                </>
            )}

            {step.type === "MATH" && (
                <>
                    <div>
                        <label className={labelCls}>Input Source</label>
                        <div className="flex flex-wrap gap-1.5">
                            {inputOptions.length === 0 && <span className="text-[11px] opacity-50">No previous steps available.</span>}
                            {inputOptions.map((opt) => {
                                const active = step.inputs.includes(opt.id);
                                return (
                                    <button key={opt.id} type="button"
                                        onClick={() => onChange({ inputs: active ? step.inputs.filter((i) => i !== opt.id) : [...step.inputs, opt.id] })}
                                        className={`text-[11px] px-2.5 py-1 rounded-full border font-semibold transition-colors ${active ? "bg-violet-600 border-violet-600 text-white" : isDark ? "border-white/10 text-slate-400 hover:bg-white/5" : "border-slate-400 text-slate-700 hover:bg-slate-100"}`}>
                                        [{opt.id}]
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                    <div>
                        <label className={labelCls}>Expression</label>
                        <input className={`w-full rounded-lg border px-3 py-2 text-sm font-mono ${inputCls}`} placeholder="#A / #B * 100" value={step.expression} onChange={(e) => onChange({ expression: e.target.value })} />
                        <p className={`text-[10px] font-medium mt-1.5 ${isDark ? "text-slate-500" : "text-slate-600"}`}>
                            Use <code className={isDark ? "text-violet-300" : "text-violet-750 font-bold"}>#</code> followed by the block ID to reference its value. Example: <code>#A + #B</code>, <code>#A / #B * 100</code>.
                        </p>
                    </div>
                </>
            )}

            {step.type === "EVALUATE_THRESHOLD" && (
                <>
                    <div>
                        <label className={labelCls}>Input Source</label>
                        <select className={`rounded-lg border px-3 py-2 text-sm ${inputCls}`} value={step.inputStepId} onChange={(e) => onChange({ inputStepId: e.target.value })}>
                            <option value="">— Select step —</option>
                            {inputOptions.map((opt) => (
                                <option key={opt.id} value={opt.id}>[{opt.id}] {opt.type === "MATH" ? "Math Expression" : "Query Logs Data"}</option>
                            ))}
                        </select>
                    </div>
                    <div className="flex items-end gap-2">
                        <div>
                            <label className={labelCls}>Condition</label>
                            <select className={`rounded-lg border px-3 py-2 text-sm ${inputCls}`} value={step.operator} onChange={(e) => onChange({ operator: e.target.value })}>
                                {THRESHOLD_OPERATORS.map((op) => <option key={op.value} value={op.value}>{op.label}</option>)}
                            </select>
                        </div>
                        <input type="number" className={`w-24 rounded-lg border px-3 py-2 text-sm ${inputCls}`} placeholder="10" value={step.value} onChange={(e) => onChange({ value: e.target.value })} />
                    </div>
                </>
            )}
        </div>
    );
}