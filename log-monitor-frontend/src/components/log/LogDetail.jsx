import { useEffect } from "react";
import { levelConfig } from "../../utils/constants.js";
import { useLogDetail } from "../../hooks/useLogDetail.js";

const FIELDS = [
    ["timestamp", (log) => log.timestamp],
    ["docId", (log) => log.docId],
    ["level", (log) => log.level],
    ["environment", (log) => log.env],
    ["service", (log) => log.service],
    ["appName", (log) => log.appName],
    ["hostName", (log) => log.hostName],
    ["logger", (log) => log.logger],
    ["thread", (log) => log.thread],
    ["traceId", (log) => log.traceId],
    ["message", (log) => log.message],
    ["sentTimestamp", (log) => log.sentTimestamp],
    ["receivedTimestamp", (log) => log.receivedTimestamp],
    ["deliveryLatencyMs", (log) => log.deliveryLatencyMs != null ? `${log.deliveryLatencyMs}ms` : null],
];

export default function LogDetail({ log, onClose, isDark }) {
    // `log` ở đây là row tóm tắt được click từ LogTable (đủ để hiện ngay các field
    // cơ bản trong lúc fetch). useLogDetail load lại full document theo docId để
    // lấy thêm stackTrace/appName/hostName/logger mà list API không trả về.
    const { detail, isLoading, isError, error } = useLogDetail(log?.docId);

    // Đóng modal bằng phím Escape, khóa scroll của trang nền khi modal mở
    useEffect(() => {
        if (!log) return;

        const handleKeyDown = (e) => {
            if (e.key === "Escape") onClose();
        };

        document.addEventListener("keydown", handleKeyDown);
        const prevOverflow = document.body.style.overflow;
        document.body.style.overflow = "hidden";

        return () => {
            document.removeEventListener("keydown", handleKeyDown);
            document.body.style.overflow = prevOverflow;
        };
    }, [log, onClose]);

    if (!log) return null;

    // Ưu tiên dữ liệu full detail; rơi về row tóm tắt trong lúc đang fetch
    const view = detail || log;
    const lvl = levelConfig[view.level] || levelConfig.INFO;

    const panelClass = isDark
        ? "bg-[#0a0f1a] border-white/10"
        : "bg-white border-slate-200";

    const rowDivider = isDark ? "border-white/4" : "border-slate-100";

    const valueColor = (key) => {
        if (key === "level") return lvl.hex;
        if (key === "message") return isDark ? "#c8cfe8" : "#1a2035";
        return isDark ? "#94a3b8" : "#374151";
    };

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center p-4"
            role="dialog"
            aria-modal="true"
            aria-label="Log detail"
        >
            {/* Backdrop — click để đóng */}
            <div
                className="absolute inset-0 bg-black/60 backdrop-blur-sm"
                onClick={onClose}
            />

            {/* Modal card — flex column: header cố định trên cùng, body cuộn riêng */}
            <div
                className={`relative flex flex-col w-full max-w-250 max-h-[85vh] border rounded-xl font-mono text-xs shadow-2xl transition-colors duration-200 ${panelClass}`}
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header — cố định, không cuộn theo nội dung, sát mép trên cùng của card */}
                <div
                    className={`flex justify-between items-center px-5 py-3.5 border-b rounded-t-xl shrink-0 ${rowDivider}`}
                >
                    <span className="font-bold text-[13px]" style={{ color: lvl.hex }}>
                        ▶ Log Detail
                    </span>
                    <div className="flex items-center gap-3">
                        {isLoading && (
                            <span className="text-slate-700 text-[11px]">loading full detail…</span>
                        )}
                        <button
                            onClick={onClose}
                            className="bg-transparent border-none text-slate-600 cursor-pointer text-xl leading-none px-1 hover:text-slate-900 transition-colors"
                        >
                            ×
                        </button>
                    </div>
                </div>

                {/* Body — vùng cuộn riêng, header luôn cố định bên trên */}
                <div className="overflow-y-auto p-5">
                    {isError && (
                        <div className="text-red-400 text-[11px] mb-3">
                            Failed to load full detail: {error?.message || "Unknown error"} — showing summary only.
                        </div>
                    )}

                    {/* Fields */}
                    {FIELDS.map(([key, getValue]) => {
                        const value = getValue(view);
                        if (value === undefined || value === null || value === "") return null;

                        return (
                            <div
                                key={key}
                                className={`grid gap-2 mb-2 border-b pb-2 ${rowDivider}`}
                                style={{ gridTemplateColumns: "110px 1fr" }}
                            >
                                <span className="text-slate-700">{key}</span>
                                <span className="break-all" style={{ color: valueColor(key) }}>
                                    {value}
                                </span>
                            </div>
                        );
                    })}

                    {/* Stack trace — chỉ có trong full detail, không có ở row tóm tắt */}
                    {view.stackTrace && (
                        <div className="mt-3">
                            <div className="text-slate-700 mb-1.5">stackTrace</div>
                            <pre
                                className={`whitespace-pre-wrap break-all rounded-lg p-3 text-[11px] leading-relaxed overflow-x-auto ${isDark ? "bg-black/30 text-red-300" : "bg-red-50 text-red-700"
                                    }`}
                            >
                                {view.stackTrace}
                            </pre>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}