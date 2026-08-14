import { useState } from "react";

import Sidebar from "./components/Sidebar";
import Topbar from "./components/Topbar";

import Dashboard from "./pages/Dashboard";
import MapPage from "./pages/MapPage";
import TeamManagement from "./pages/TeamManagement";
import AssignTeam from "./pages/AssignTeam";
import Notifications from "./pages/Notifications";

function App() {
  const [activePage, setActivePage] = useState("dashboard");

  const pageTitles: Record<string, string> = {
    dashboard: "Dashboard",
    map: "Live Map",
    teams: "Team Management",
    assign: "Assign Rescue Team",
    notifications: "Notifications",
  };

  const renderPage = () => {
    switch (activePage) {
      case "map":
        return <MapPage />;

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
        return <Dashboard />;
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
        />

        <main className="flex-1 overflow-auto p-6 lg:p-8">
          {renderPage()}
        </main>
      </div>
    </div>
  );
}

export default App;