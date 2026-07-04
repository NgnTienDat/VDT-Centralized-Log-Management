import { useState, useEffect, useRef } from "react";

export default function SearchInput({ value, onChange, isDark }) {
    const [localValue, setLocalValue] = useState(value || "");

    const onChangeRef = useRef(onChange);
    useEffect(() => {
        onChangeRef.current = onChange;
    }, [onChange]);

    // Sync ngược khi value bị reset từ bên ngoài (ví dụ clear all filters)
    useEffect(() => {
        setLocalValue(value || "");
    }, [value]);

    // isMounted guard — tránh bắn onChange("") ngay khi component mount
    const isMounted = useRef(false);
    useEffect(() => {
        if (!isMounted.current) {
            isMounted.current = true;
            return;
        }
        const timer = setTimeout(() => {
            if (typeof onChangeRef.current === "function") {
                onChangeRef.current(localValue);
            }
        }, 500);
        return () => clearTimeout(timer);
    }, [localValue]);

    return (
        <div className="relative flex-1 min-w-48">
            <span className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-500 text-sm pointer-events-none">
                ⌕
            </span>
            <input
                value={localValue}
                onChange={(e) => setLocalValue(e.target.value)}
                placeholder="Search messages, traceId, service..."
                className={[
                    "w-full rounded-md py-1.5 pl-8 pr-3 text-xs font-mono outline-none",
                    "border transition-colors duration-200",
                    isDark
                        ? "bg-white/4 border-white/8 text-slate-200 placeholder-slate-600 focus:border-white/20"
                        : "bg-white border-slate-200 text-slate-900 placeholder-slate-500 focus:border-indigo-500",
                ].join(" ")}
            />
        </div>
    );
}