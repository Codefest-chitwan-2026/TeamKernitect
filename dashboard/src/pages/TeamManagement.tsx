import { useEffect, useState } from "react";
import TeamTable from "../components/TeamTable";
import { approveResponder, getRescueTeams, getResponders, rejectResponder, type ResponderRegistration, type RescueTeam } from "../services/api";

export default function TeamManagement({ onAssign }: { onAssign: () => void }) {
  const [teams, setTeams] = useState<RescueTeam[]>([]);
  const [responders, setResponders] = useState<ResponderRegistration[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const load = async () => { setLoading(true); setError(null); try { const [teamData, responderData] = await Promise.all([getRescueTeams(), getResponders()]); setTeams(teamData.items); setResponders(responderData.items); } catch (value) { setError(value instanceof Error ? value.message : "Unable to load responder management"); } finally { setLoading(false); } };
  useEffect(() => { const id = window.setTimeout(() => void load(), 0); return () => window.clearTimeout(id); }, []);
  const approve = async (item: ResponderRegistration) => { try { await approveResponder(item.responderId); await load(); } catch (value) { setError(value instanceof Error ? value.message : "Approval failed"); } };
  const reject = async (item: ResponderRegistration) => { try { await rejectResponder(item.responderId); await load(); } catch (value) { setError(value instanceof Error ? value.message : "Rejection failed"); } };
  const pending = responders.filter(item => item.status === "PENDING");
  return <div className="space-y-6">
    <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end"><div><h2 className="text-2xl font-bold">Team Management</h2><p className="mt-1 text-sm text-gray-500">Rescue-team registration and administrator verification.</p></div><button onClick={onAssign} className="rounded-lg bg-red-500 px-5 py-2.5 text-sm font-semibold text-white">+ Assign Team</button></div>
    {error && <div className="rounded-xl bg-red-50 p-4 text-sm text-red-700">{error}</div>}
    {loading ? <p className="text-sm text-gray-500">Loading teams and responders...</p> : <>
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">{[["Official Teams", teams.length], ["Pending", pending.length], ["Approved", responders.filter(i => i.status === "APPROVED").length], ["Rejected", responders.filter(i => i.status === "REJECTED").length]].map(([label, value]) => <div key={label} className="rounded-xl border border-gray-200 bg-white p-4"><p className="text-xs text-gray-500">{label}</p><p className="mt-1 text-2xl font-bold">{value}</p></div>)}</div>
      <TeamTable teams={teams} />
      <section className="rounded-xl border border-gray-200 bg-white shadow-sm"><div className="border-b border-gray-100 px-5 py-4"><h3 className="font-semibold">Pending Team Approvals</h3></div>{pending.length === 0 ? <p className="p-5 text-sm text-gray-500">No pending team registrations.</p> : pending.map(item => <div key={item.responderId} className="grid gap-4 border-b border-gray-100 p-5 lg:grid-cols-[1fr_auto]"><div className="grid gap-2 sm:grid-cols-2"><Info label="Team Name" value={item.teamName || item.organization} /><Info label="Callsign" value={item.callsign || "—"} /><Info label="District" value={item.district} /><Info label="Leader Name" value={item.leaderName || item.operatorName} /><Info label="Leader Phone" value={item.leaderPhone || item.phone} /><Info label="Leader Email" value={item.leaderEmail || item.email || "—"} /><Info label="Status" value="PENDING" /></div><div className="flex items-center gap-2"><button onClick={() => void approve(item)} className="rounded-lg bg-red-500 px-4 py-2 text-xs font-semibold text-white">APPROVE</button><button onClick={() => void reject(item)} className="rounded-lg border px-4 py-2 text-xs font-semibold">REJECT</button></div></div>)}</section>
      <section className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">{responders.filter(i => i.status !== "PENDING").map(item => <article key={item.responderId} className="rounded-xl border border-gray-200 bg-white p-4"><span className={`text-xs font-bold ${item.status === "APPROVED" ? "text-green-600" : "text-red-600"}`}>{item.status === "APPROVED" ? "VERIFIED • APPROVED" : "REJECTED"}</span><h3 className="mt-1 font-semibold">{item.teamName || item.organization}</h3><p className="text-sm text-gray-500">{item.callsign || "No callsign"} • {item.district}</p><p className="mt-2 text-xs text-gray-400">{item.leaderName || item.operatorName} • {item.leaderEmail || item.email}</p></article>)}</section>
    </>}
  </div>;
}

function Info({ label, value }: { label: string; value: string }) { return <div><p className="text-xs text-gray-400">{label}</p><p className="text-sm font-semibold text-gray-700">{value}</p></div>; }
