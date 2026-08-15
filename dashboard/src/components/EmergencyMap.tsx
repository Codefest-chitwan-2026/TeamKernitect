import { MapContainer, Marker, Popup, TileLayer } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import type { Sos } from "../services/api";

const markerIcon = (color: string, symbol: string) => L.divIcon({ className: "", html: `<div style="width:34px;height:34px;border-radius:50%;background:${color};border:3px solid white;box-shadow:0 3px 10px #0004;display:flex;align-items:center;justify-content:center;color:white;font-weight:800">${symbol}</div>`, iconSize: [34, 34], iconAnchor: [17, 17] });
const victimIcon = markerIcon("#ef4444", "!");
const responderIcon = markerIcon("#2563eb", "+");
const valid = (lat?: number | null, lon?: number | null) => lat != null && lon != null && lat !== 0 && lon !== 0 && Math.abs(lat) <= 90 && Math.abs(lon) <= 180;

export default function EmergencyMap({ sosItems = [] }: { sosItems?: Sos[] }) {
  return <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
    <div className="flex items-center justify-between border-b border-gray-100 px-5 py-4"><div><h2 className="font-semibold text-gray-900">Emergency Map</h2><p className="mt-1 text-xs text-gray-500">Victim locations and latest synchronized responder snapshots</p></div><div className="hidden gap-4 text-xs text-gray-600 sm:flex"><span>🔴 Victim</span><span>🔵 Responder snapshot</span></div></div>
    <div className="h-125 w-full"><MapContainer center={[28.3949, 84.124]} zoom={7} scrollWheelZoom className="h-full w-full"><TileLayer attribution='&copy; OpenStreetMap contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
      {sosItems.filter(item => valid(item.latitude, item.longitude)).map(item => <Marker key={`victim-${item.id}`} position={[item.latitude, item.longitude]} icon={victimIcon}><Popup><strong>{item.id}</strong><p>{item.message || "Emergency"}</p><p>{item.currentLifecycleStatus?.replaceAll("_", " ") || "Awaiting responder"}</p></Popup></Marker>)}
      {sosItems.filter(item => valid(item.lastResponderLatitude, item.lastResponderLongitude)).map(item => <Marker key={`responder-${item.id}`} position={[item.lastResponderLatitude!, item.lastResponderLongitude!]} icon={responderIcon}><Popup><strong>{item.assignedTeamName || "Responder team"}</strong><p>{item.callsign}</p><p>Latest synchronized snapshot — not live tracking</p></Popup></Marker>)}
    </MapContainer></div>
  </div>;
}
