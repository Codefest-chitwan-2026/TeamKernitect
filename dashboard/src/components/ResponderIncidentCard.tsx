import type { RescueLifecycle, Sos } from "../services/api";

const steps: Array<[RescueLifecycle, string, keyof Sos]> = [
  ["ACCEPTED", "Accepted", "acceptedAt"], ["ON_THE_WAY", "On the Way", "onTheWayAt"],
  ["NEARBY", "Nearby", "nearbyAt"], ["ARRIVED", "Arrived", "arrivedAt"], ["RESCUED", "Rescued", "rescuedAt"],
];
const rank = (value?: RescueLifecycle | null) => value ? steps.findIndex(([status]) => status === value) : -1;
const formatTime = (value?: number | string | null) => value ? new Date(typeof value === "number" ? value : value).toLocaleString() : "";
const validCoordinate = (value?: number | null) => value != null && value !== 0 && Number.isFinite(value);

export default function ResponderIncidentCard({ incident }: { incident: Sos }) {
  const currentRank = rank(incident.currentLifecycleStatus);
  return <article className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div><span className="text-xs font-bold text-red-500">{incident.priority}</span><h3 className="font-semibold text-gray-900">{incident.id}</h3><p className="mt-1 text-sm text-gray-500">{incident.message || "Emergency assistance requested"}</p></div>
      <span className={`rounded-full px-3 py-1 text-xs font-semibold ${incident.claimConflict ? "bg-amber-100 text-amber-700" : incident.assignedTeamId ? "bg-blue-50 text-blue-700" : "bg-gray-100 text-gray-500"}`}>
        {incident.claimConflict ? "Claim Conflict" : incident.currentLifecycleStatus?.replaceAll("_", " ") || "Unassigned"}
      </span>
    </div>
    <div className="mt-4 grid gap-4 lg:grid-cols-[0.8fr_1.2fr]">
      <div className="rounded-lg bg-gray-50 p-4 text-sm">
        <p className="text-xs font-semibold uppercase text-gray-400">Assigned Team</p>
        <p className="mt-1 font-semibold text-gray-800">{incident.assignedTeamName || "Unassigned"}</p>
        {incident.callsign && <p className="text-gray-500">{incident.callsign}</p>}
        {incident.claimConflict && <div className="mt-3 rounded-lg bg-amber-50 p-3 text-xs text-amber-800">Multiple rescue teams reported this incident: {incident.claimTeamIds?.join(" / ") || "details pending"}. No winner has been selected.</div>}
        <p className="mt-3 text-xs text-gray-500">Last synchronized: {formatTime(incident.lastResponderSyncAt || (incident.assignedTeamId ? incident.updatedAt : null)) || "No responder sync yet"}</p>
        {validCoordinate(incident.lastResponderLatitude) && validCoordinate(incident.lastResponderLongitude) && <p className="mt-1 text-xs text-gray-500">Latest responder snapshot: {incident.lastResponderLatitude?.toFixed(5)}, {incident.lastResponderLongitude?.toFixed(5)}</p>}
      </div>
      <div><p className="mb-2 text-xs font-semibold uppercase text-gray-400">Rescue Progress</p><div className="space-y-2">
        {steps.map(([status, label, timestampField], index) => { const reached = index <= currentRank; const timestamp = incident[timestampField] as number | undefined; return <div key={status} className="flex items-center justify-between gap-3 text-sm"><span className={reached ? "font-medium text-gray-800" : "text-gray-400"}>{index < currentRank ? "✓" : index === currentRank ? "●" : "○"} {label}</span><span className="text-xs text-gray-500">{reached && timestamp ? formatTime(timestamp) : ""}</span></div>; })}
      </div></div>
    </div>
  </article>;
}
