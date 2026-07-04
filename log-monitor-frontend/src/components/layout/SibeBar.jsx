import { useState } from "react";

export default function SideBar({ activeTab, setActiveTab, isDark }) {
    const sidebarBg = isDark ? "bg-[#0a0f1a] border-white/5" : "bg-white border-slate-200 shadow-xs";
    const textTitle = isDark ? "text-slate-400" : "text-slate-500";

    const menuItems = [
        { id: "logs", label: "Logs Explorer" },
        { id: "alerts", label: "Alert Rules" },
    ];

    return (
        <aside className={`w-64 h-screen flex flex-col border-r transition-colors duration-200 ${sidebarBg}`}>
            {/* Brand Logo Section */}
            <div className="p-5 flex items-center gap-3 border-b border-inherit">
                <div className="w-9 h-9 rounded-xl bg-linear-to-br from-sky-500 to-indigo-600 flex items-center justify-center text-lg shadow-md shadow-indigo-500/20 text-white">
                    🛰️
                </div>
                <div>
                    <h1 className="text-sm font-bold tracking-tight text-slate-800 dark:text-slate-200">
                        Log<span className="text-indigo-500">Radar</span>
                    </h1>
                    <p className={`text-[10px] font-mono ${textTitle}`}>v1.0.0</p>
                </div>
            </div>

            {/* Navigation Tabs */}
            <nav className="flex-1 p-4 flex flex-col gap-1.5">
                <span className={`text-[10px] uppercase font-bold tracking-wider px-2 mb-2 block ${textTitle}`}>
                    Main Modules
                </span>
                {menuItems.map((item) => {
                    const isActive = activeTab === item.id;
                    return (
                        <button
                            key={item.id}
                            onClick={() => setActiveTab(item.id)}
                            className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-semibold transition-all duration-150 text-left cursor-pointer ${isActive
                                ? "bg-indigo-600 text-white shadow-sm"
                                : isDark
                                    ? "text-slate-400 hover:bg-white/5 hover:text-slate-200"
                                    : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                                }`}
                        >
                            <span>{item.label}</span>
                            {isActive && <span className="ml-auto text-[10px]">●</span>}
                        </button>
                    );
                })}
            </nav>

            {/* System Info Footer */}
            <div className="p-4 border-t border-inherit text-[10px] font-mono text-center text-slate-500">
                VDT Centralized Log
            </div>
        </aside>
    );
}