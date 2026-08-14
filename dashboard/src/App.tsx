import { useEffect, useState } from "react";

import {
  getSosList,
  getSosStats,
  type Sos,
  type SosStats,
} from "./services/api";


function App() {
  const [sosItems, setSosItems] = useState<Sos[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [stats, setStats] = useState<SosStats | null>(null);



  useEffect(() => {
 async function loadSos() {
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
}

    loadSos();
  }, []);


  if (loading) {
    return <p>Loading Sahara emergencies...</p>;
  }


  if (error) {
    return <p>Error: {error}</p>;
  }


  return (
    <div>
      <h1>Sahara Rescue Dashboard</h1>
      {stats && (
  <div>
    <h2>Emergency Statistics</h2>

    <p>Total SOS: {stats.total}</p>
    <p>New: {stats.new}</p>
    <p>Responding: {stats.responding}</p>
    <p>Resolved: {stats.resolved}</p>
    <p>Critical Active: {stats.criticalActive}</p>
  </div>
)}
      <h2>
        Active SOS Messages: {sosItems.length}
      </h2>

      {sosItems.map((sos) => (
        <div key={sos.id}>
          <hr />

          <h3>
            {sos.type} — {sos.priority}
          </h3>

          <p>
            ID: {sos.id}
          </p>

          <p>
            Status: {sos.status}
          </p>

          <p>
            Location: {sos.latitude}, {sos.longitude}
          </p>

          <p>
            Message: {sos.message ?? "No description"}
          </p>

          <p>
            Hop Count: {sos.hopCount}
          </p>
        </div>
      ))}
    </div>
  );
}


export default App;