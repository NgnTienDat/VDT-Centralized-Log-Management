import SearchInput from "./SearchInput.jsx";
import LevelFilter from "./LevelFilter.jsx";
import EnvFilter from "./EnvFilter.jsx";
import ServiceFilter from "./ServiceFilter.jsx";
import AppFilter from "./AppFilter.jsx";

export default function FilterBar({
    search, onSearch,
    filterLevel, onLevel,
    filterEnv, onEnv,
    filterService, onService,
    filterApp, onApp,
    services,
    apps,
    filteredCount, totalCount,
    isDark,
}) {
    const panelClass = isDark
        ? "bg-[#0a0f1a] border-white/6"
        : "bg-white border-slate-200 shadow-sm";

    const dividerClass = isDark ? "border-white/6" : "border-slate-200";

    return (
        <div className={`border rounded-xl mb-1 transition-colors duration-200 ${panelClass}`}>
            {/* Row 1: search + level */}
            <div className={`px-4 py-3 border-b flex items-center gap-3.5 flex-wrap ${dividerClass}`}>
                <SearchInput value={search} onChange={onSearch} isDark={isDark} />
                <LevelFilter value={filterLevel} onChange={onLevel} isDark={isDark} />
            </div>

            {/* Row 2: env + service + count */}
            <div className="px-4 py-2.5 flex items-center gap-2.5 flex-wrap">
                <EnvFilter value={filterEnv} onChange={onEnv} isDark={isDark} />

                {/* 🌟 TRUYỀN MẢNG SERVICES XUỐNG COMPONENT CON */}
                <ServiceFilter
                    value={filterService}
                    onChange={onService}
                    services={services}
                    isDark={isDark}
                />

                <AppFilter
                    value={filterApp}
                    onChange={onApp}
                    apps={apps}
                    isDark={isDark}
                />

                <span className="ml-auto text-[11px] text-slate-700 font-mono">
                    Showing{" "}
                    <span className={isDark ? "text-sky-400" : "text-sky-600"}>{filteredCount}</span>
                    {" / "}
                    {totalCount} logs
                </span>
            </div>
        </div>
    );
}