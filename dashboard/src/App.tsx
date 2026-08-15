import { useCallback, useEffect, useState } from "react";

import {
  getSosList,
  getSosStats,
  type Sos,
  type SosStats,
} from "./services/api";

import Sidebar from "./components/Sidebar";
import Topbar from "./components/Topbar";

import Dashboard from "./pages/Dashboard";
import MapPage from "./pages/MapPage";
import TeamManagement from "./pages/TeamManagement";
import AssignTeam from "./pages/AssignTeam";
import Notifications from "./pages/Notifications";

function App() {
  const [activePage, setActivePage] = useState("dashboard");

  const [sosItems, setSosItems] = useState<Sos[]>([]);
  const [stats, setStats] = useState<SosStats | null>(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const pageTitles: Record<string, string> = {
    dashboard: "Dashboard",
    map: "Live Map",
    teams: "Team Management",
    assign: "Assign Rescue Team",
    notifications: "Notifications",
  };

  const loadSos = useCallback(async () => {
      try {
        const [sosData, statsData] = await Promise.all([
          getSosList(),
          getSosStats(),
        ]);

        setSosItems(sosData.items);
        setStats(statsData);
      } catch (err) {
        setError(
          err instanceof Error
            ? err.message
            : "Unknown error"
        );
      } finally {
        setLoading(false);
      }
  }, []);
  useEffect(() => { void loadSos(); const timer = window.setInterval(() => void loadSos(), 20_000); return () => window.clearInterval(timer); }, [loadSos]);

  const renderPage = () => {
    if (loading) {
      return (
        <div className="flex min-h-[300px] items-center justify-center">
          <p className="text-sm text-gray-500">
            Loading Sahara emergencies...
          </p>
        </div>
      );
    }

    if (error) {
      return (
        <div className="rounded-xl border border-red-200 bg-red-50 p-6">
          <p className="font-medium text-red-600">
            Unable to load emergency data
          </p>

          <p className="mt-1 text-sm text-red-500">
            {error}
          </p>
        </div>
      );
    }

    switch (activePage) {
      case "map":
        return <MapPage sosItems={sosItems} />;

      case "teams":
        return (
          <TeamManagement
            onAssign={() => setActivePage("assign")}
          />
        );

      case "assign":
        return (
          <AssignTeam
            onBack={() => setActivePage("teams")}
          />
        );

      case "notifications":
        return <Notifications />;

      default:
        return (
          <Dashboard
            sosItems={sosItems}
            stats={stats}
          />
        );
    }
  };

  return (
    <div className="flex min-h-screen bg-gray-50 text-gray-900">

      <Sidebar
        activePage={activePage}
        onNavigate={setActivePage}
      />

      <div className="flex min-w-0 flex-1 flex-col">

        <Topbar
          title={pageTitles[activePage] ?? "Dashboard"}
          onRefresh={() => void loadSos()}
        />

        <main className="flex-1 overflow-auto p-6 lg:p-8">
          {renderPage()}
        </main>

      </div>
    </div>
  );
}

export default App;
