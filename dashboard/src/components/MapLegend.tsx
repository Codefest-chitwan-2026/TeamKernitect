export default function MapLegend() {
  return (
    <div className="flex flex-wrap items-center gap-5">
      <div className="flex items-center gap-2">
        <span className="h-3 w-3 rounded-full bg-red-500 ring-2 ring-red-100" />
        <span className="text-xs font-medium text-gray-600">
          Victim
        </span>
      </div>

      <div className="flex items-center gap-2">
        <span className="h-3 w-3 rounded-full bg-blue-600 ring-2 ring-blue-100" />
        <span className="text-xs font-medium text-gray-600">
          Rescue Team
        </span>
      </div>

      <div className="flex items-center gap-2">
        <span className="h-3 w-3 rounded-full bg-green-500 ring-2 ring-green-100" />
        <span className="text-xs font-medium text-gray-600">
          Rescued
        </span>
      </div>
    </div>
  );
}