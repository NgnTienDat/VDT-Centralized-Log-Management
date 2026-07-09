import { useState } from "react";
import SideBar from "./components/layout/SibeBar";
import AppHeader from "./components/layout/AppHeader";
import LogDashboard from "./pages/LogDashboard";
import AlertMonitor from "./pages/AlertMonitor";
import { ToastContainer } from "react-toastify";

export default function App() {
  const [isDark, setIsDark] = useState(false);
  const [activeTab, setActiveTab] = useState("logs"); // Quản lý tab hiện tại: 'logs' hoặc 'alerts'

  const globalThemeClass = isDark
    ? "dark bg-[#070b14] text-slate-200"
    : "bg-slate-50 text-slate-900";

  return (
    <div className={`w-screen h-screen flex overflow-hidden transition-colors duration-200 ${globalThemeClass}`}>
      <ToastContainer
        position="bottom-right"
        autoClose={5000}
        newestOnTop
        closeOnClick
        pauseOnHover
        draggable
        theme={isDark ? "dark" : "light"}
      />

      {/* Left Fixed Navigation Menu */}
      <SideBar activeTab={activeTab} setActiveTab={setActiveTab} isDark={isDark} />

      {/* Right Master Workspace Container */}
      <div className="flex-1 flex flex-col overflow-hidden">

        {/* Top Unified Header Dashboard */}
        <AppHeader isDark={isDark} onToggleTheme={() => setIsDark(!isDark)} />

        {/* Sub-window View Workspace Viewports */}
        <main className="flex-1 overflow-y-auto p-6">
          {activeTab === "logs" ? (
            <LogDashboard isDark={isDark} />
          ) : (
            <AlertMonitor isDark={isDark} />
          )}
        </main>
      </div>
    </div>
  );
}