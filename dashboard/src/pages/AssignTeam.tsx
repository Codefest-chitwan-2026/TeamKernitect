interface AssignTeamProps {
  onBack: () => void;
}

export default function AssignTeam({
  onBack,
}: AssignTeamProps) {
  return (
    <div className="space-y-6">
      <div>
        <button
          onClick={onBack}
          className="mb-3 text-sm font-medium text-gray-500 hover:text-red-500"
        >
          ← Back to Team Management
        </button>

        <h2 className="text-2xl font-bold text-gray-900">
          Assign Rescue Team
        </h2>

        <p className="mt-1 text-sm text-gray-500">
          Deploy an available team to an emergency location.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[1fr_1.5fr]">
        {/* Form */}
        <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <h3 className="font-semibold text-gray-900">
            Assignment Details
          </h3>

          <div className="mt-6 space-y-5">
            <div>
              <label className="mb-2 block text-xs font-semibold text-gray-600">
                Rescue Team
              </label>

              <select className="h-11 w-full rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-700 outline-none focus:border-red-400 focus:ring-2 focus:ring-red-100">
                <option>Select available team</option>
                <option>Kathmandu Response Unit — K-01</option>
                <option>Bhaktapur Response Unit — B-03</option>
              </select>
            </div>

            <div>
              <label className="mb-2 block text-xs font-semibold text-gray-600">
                Emergency Location
              </label>

              <select className="h-11 w-full rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-700 outline-none focus:border-red-400 focus:ring-2 focus:ring-red-100">
                <option>Select emergency location</option>
                <option>Emergency Site A — Kathmandu</option>
                <option>Emergency Site B — Lalitpur</option>
                <option>Emergency Site C — Bhaktapur</option>
              </select>
            </div>

            <div>
              <label className="mb-2 block text-xs font-semibold text-gray-600">
                Priority
              </label>

              <div className="grid grid-cols-3 gap-2">
                <button className="rounded-lg border border-red-200 bg-red-50 py-2.5 text-xs font-semibold text-red-600">
                  Critical
                </button>

                <button className="rounded-lg border border-gray-200 py-2.5 text-xs font-medium text-gray-500 hover:bg-gray-50">
                  High
                </button>

                <button className="rounded-lg border border-gray-200 py-2.5 text-xs font-medium text-gray-500 hover:bg-gray-50">
                  Normal
                </button>
              </div>
            </div>

            <div>
              <label className="mb-2 block text-xs font-semibold text-gray-600">
                Assignment Note
              </label>

              <textarea
                rows={4}
                placeholder="Add instructions for the rescue team..."
                className="w-full resize-none rounded-lg border border-gray-200 p-3 text-sm outline-none placeholder:text-gray-400 focus:border-red-400 focus:ring-2 focus:ring-red-100"
              />
            </div>

            <button className="w-full rounded-lg bg-red-500 py-3 text-sm font-semibold text-white shadow-sm shadow-red-200 transition hover:bg-red-600">
              Confirm Assignment
            </button>
          </div>
        </div>

        {/* Map preview */}
        <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
          <div className="border-b border-gray-100 px-5 py-4">
            <h3 className="font-semibold text-gray-900">
              Location Preview
            </h3>

            <p className="mt-1 text-xs text-gray-500">
              Select an emergency location to preview it on the map.
            </p>
          </div>

          <div className="relative h-[520px] bg-[#eee8dc]">
            <div className="absolute left-[-10%] top-[35%] h-5 w-[120%] rotate-12 bg-white/90" />
            <div className="absolute left-[-10%] top-[65%] h-4 w-[120%] -rotate-6 bg-white/80" />
            <div className="absolute left-[35%] top-[-20%] h-[150%] w-4 rotate-[25deg] bg-white/80" />

            <div className="absolute left-[25%] top-[30%] h-24 w-40 rounded-lg bg-[#ddd5c6]" />
            <div className="absolute left-[55%] top-[55%] h-28 w-48 rounded-lg bg-[#e0d8ca]" />

            <div className="absolute left-[52%] top-[42%] flex h-10 w-10 items-center justify-center rounded-full bg-red-500 text-lg font-bold text-white shadow-lg ring-4 ring-red-500/20">
              !
            </div>

            <div className="absolute bottom-5 left-5 rounded-lg bg-white/95 px-4 py-3 shadow-md">
              <p className="text-xs font-semibold text-gray-800">
                Emergency location
              </p>

              <p className="mt-1 text-[11px] text-gray-500">
                Awaiting location selection
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}