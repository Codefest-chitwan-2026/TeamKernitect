import EmergencyMap from "../components/EmergencyMap";

export default function MapPage() {
  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-gray-900">
          Live Map
        </h2>

        <p className="mt-1 text-sm text-gray-500">
          Monitor victims, rescue teams and completed rescues.
        </p>
      </div>

      <EmergencyMap />
    </div>
  );
}