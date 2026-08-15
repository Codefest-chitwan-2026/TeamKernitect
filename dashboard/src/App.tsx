import { useEffect, useState } from "react";
import "./App.css";
import {
  approveResponder, getRescueTeams, getResponders, getSosList, getSosStats,
  rejectResponder, type RescueTeam, type ResponderRegistration, type Sos, type SosStats,
} from "./services/api";

function App() {
  const [view, setView] = useState<"emergencies" | "responders">("emergencies");
  const [sosItems, setSosItems] = useState<Sos[]>([]);
  const [stats, setStats] = useState<SosStats | null>(null);
  const [responders, setResponders] = useState<ResponderRegistration[]>([]);
  const [teams, setTeams] = useState<RescueTeam[]>([]);
  const [teamSelections, setTeamSelections] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [workingId, setWorkingId] = useState<string | null>(null);

  async function loadAll() {
    setLoading(true); setError(null);
    try {
      const [sos, sosStats, registrations, rescueTeams] = await Promise.all([
        getSosList(), getSosStats(), getResponders(), getRescueTeams(),
      ]);
      setSosItems(sos.items); setStats(sosStats);
      setResponders(registrations.items); setTeams(rescueTeams.items);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Unable to load SAHARA data.");
    } finally { setLoading(false); }
  }

  useEffect(() => { void loadAll(); }, []);

  async function approve(registration: ResponderRegistration) {
    const teamId = teamSelections[registration.responderId];
    if (!teamId) { setError("Select an official rescue team before approval."); return; }
    setWorkingId(registration.responderId); setError(null);
    try { await approveResponder(registration.responderId, teamId); await loadAll(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Approval failed."); }
    finally { setWorkingId(null); }
  }

  async function reject(registration: ResponderRegistration) {
    setWorkingId(registration.responderId); setError(null);
    try { await rejectResponder(registration.responderId); await loadAll(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Rejection failed."); }
    finally { setWorkingId(null); }
  }

  return (
    <div className="shell">
      <aside>
        <div className="brand">SAHARA <span>Command</span></div>
        <button className={view === "emergencies" ? "active" : ""} onClick={() => setView("emergencies")}>Emergencies</button>
        <button className={view === "responders" ? "active" : ""} onClick={() => setView("responders")}>Responders</button>
      </aside>
      <main>
        <header><div><small>SAHARA ADMIN</small><h1>{view === "responders" ? "Responder Management" : "Emergency Overview"}</h1></div><button onClick={() => void loadAll()}>Refresh</button></header>
        {error && <div className="error">{error}</div>}
        {loading ? <div className="empty">Loading command data…</div> : view === "responders" ? (
          <>
            <section><h2>Pending Requests</h2>
              {responders.filter(r => r.status === "PENDING").length === 0 ? <div className="empty">No pending responder requests.</div> :
                responders.filter(r => r.status === "PENDING").map(r => (
                  <article className="request" key={r.responderId}>
                    <div><span className="pill pending">PENDING</span><h3>{r.operatorName}</h3><p>{r.organization} • {r.district}</p><p>{r.phone}{r.email ? ` • ${r.email}` : ""}</p><code>{r.deviceId}</code></div>
                    <div className="actions"><select value={teamSelections[r.responderId] ?? ""} onChange={e => setTeamSelections(s => ({...s, [r.responderId]: e.target.value}))}><option value="">Assign rescue team…</option>{teams.map(t => <option key={t.teamId} value={t.teamId}>{t.teamName} — {t.callsign}</option>)}</select><button disabled={workingId === r.responderId} onClick={() => void approve(r)}>Approve & Assign</button><button className="secondary" disabled={workingId === r.responderId} onClick={() => void reject(r)}>Reject</button></div>
                  </article>
                ))}
            </section>
            <section><h2>Official Rescue Teams</h2><div className="grid">{teams.map(t => <article className="card" key={t.teamId}><span className="pill approved">{t.status}</span><h3>{t.teamName}</h3><strong>{t.callsign}</strong><p>{t.district}, {t.province}</p><code>{t.teamId}</code></article>)}</div></section>
            <section><h2>Registration History</h2><div className="grid">{responders.filter(r => r.status !== "PENDING").map(r => <article className="card" key={r.responderId}><span className={`pill ${r.status.toLowerCase()}`}>{r.status}</span><h3>{r.operatorName}</h3><p>{r.organization}</p><strong>{r.teamName ?? "No team assigned"}</strong><p>{r.responderId}</p></article>)}</div></section>
          </>
        ) : (
          <><section className="stats">{stats && <><div><b>{stats.total}</b><span>Total SOS</span></div><div><b>{stats.new}</b><span>New</span></div><div><b>{stats.responding}</b><span>Responding</span></div><div><b>{stats.criticalActive}</b><span>Critical</span></div></>}</section><section><h2>Active SOS Messages</h2><div className="grid">{sosItems.map(s => <article className="card" key={s.id}><span className="pill pending">{s.priority}</span><h3>{s.type}</h3><p>{s.message ?? "No description"}</p><p>{s.latitude}, {s.longitude}</p><strong>{s.status}</strong></article>)}</div></section></>
        )}
      </main>
    </div>
  );
}
export default App;
