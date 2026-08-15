import EmergencyMap from "../components/EmergencyMap";
import type { Sos } from "../services/api";

export default function MapPage({ sosItems }: { sosItems: Sos[] }) {
  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-gray-900">
          Operations Map
        </h2>

        <p className="mt-1 text-sm text-gray-500">
          Victim locations and latest synchronized responder snapshots; this is not live tracking.
        </p>
      </div>

      <EmergencyMap sosItems={sosItems} />
    </div>
  );
}
