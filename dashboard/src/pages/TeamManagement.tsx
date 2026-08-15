import TeamTable from "../components/TeamTable";

interface TeamManagementProps {
  onAssign: () => void;
}

export default function TeamManagement({
  onAssign,
}: TeamManagementProps) {
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">
            Team Management
          </h2>

          <p className="mt-1 text-sm text-gray-500">
            Manage rescue teams and their current assignments.
          </p>
        </div>

        <button
          onClick={onAssign}
          className="rounded-lg bg-red-500 px-5 py-2.5 text-sm font-semibold text-white shadow-sm shadow-red-200 transition hover:bg-red-600"
        >
          + Assign Team
        </button>
      </div>

      {/* Search */}
      <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
        <div className="flex flex-col gap-3 md:flex-row">
          <input
            type="text"
            placeholder="Search rescue team..."
            className="h-10 flex-1 rounded-lg border border-gray-200 px-4 text-sm outline-none transition placeholder:text-gray-400 focus:border-red-400 focus:ring-2 focus:ring-red-100"
          />

          <select className="h-10 rounded-lg border border-gray-200 bg-white px-4 text-sm text-gray-600 outline-none focus:border-red-400">
            <option>All Status</option>
            <option>Available</option>
            <option>On Mission</option>
            <option>Offline</option>
          </select>

          <button className="h-10 rounded-lg bg-gray-900 px-5 text-sm font-medium text-white hover:bg-gray-800">
            Search
          </button>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <div className="rounded-xl border border-gray-200 bg-white p-4">
          <p className="text-xs text-gray-500">Total Teams</p>
          <p className="mt-1 text-2xl font-bold">08</p>
        </div>

        <div className="rounded-xl border border-gray-200 bg-white p-4">
          <p className="text-xs text-gray-500">Available</p>
          <p className="mt-1 text-2xl font-bold text-green-600">05</p>
        </div>

        <div className="rounded-xl border border-gray-200 bg-white p-4">
          <p className="text-xs text-gray-500">On Mission</p>
          <p className="mt-1 text-2xl font-bold text-blue-600">03</p>
        </div>

        <div className="rounded-xl border border-gray-200 bg-white p-4">
          <p className="text-xs text-gray-500">Offline</p>
          <p className="mt-1 text-2xl font-bold text-gray-400">02</p>
        </div>
      </div>

      <TeamTable />
    </div>
  );
}