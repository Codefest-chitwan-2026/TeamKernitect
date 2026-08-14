const teams = [
  {
    id: "K-01",
    name: "Kathmandu Response Unit",
    members: "06 members",
    location: "Kathmandu",
    status: "Available",
  },
  {
    id: "L-02",
    name: "Lalitpur Rescue Unit",
    members: "04 members",
    location: "Lalitpur",
    status: "On Mission",
  },
  {
    id: "B-03",
    name: "Bhaktapur Response Unit",
    members: "05 members",
    location: "Bhaktapur",
    status: "Available",
  },
  {
    id: "K-04",
    name: "Rapid Response Team",
    members: "07 members",
    location: "Kathmandu",
    status: "Offline",
  },
];

export default function TeamTable() {
  return (
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
      <div className="overflow-x-auto">
        <table className="w-full min-w-[700px] text-left">
          <thead className="border-b border-gray-200 bg-gray-50">
            <tr>
              <th className="px-5 py-4 text-xs font-semibold uppercase tracking-wide text-gray-500">
                Team
              </th>

              <th className="px-5 py-4 text-xs font-semibold uppercase tracking-wide text-gray-500">
                Members
              </th>

              <th className="px-5 py-4 text-xs font-semibold uppercase tracking-wide text-gray-500">
                Location
              </th>

              <th className="px-5 py-4 text-xs font-semibold uppercase tracking-wide text-gray-500">
                Status
              </th>

              <th className="px-5 py-4 text-xs font-semibold uppercase tracking-wide text-gray-500">
                Action
              </th>
            </tr>
          </thead>

          <tbody className="divide-y divide-gray-100">
            {teams.map((team) => (
              <tr
                key={team.id}
                className="transition hover:bg-red-50/40"
              >
                <td className="px-5 py-4">
                  <div className="flex items-center gap-3">
                    <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-red-50 text-xs font-bold text-red-600">
                      {team.id}
                    </div>

                    <div>
                      <p className="text-sm font-semibold text-gray-800">
                        {team.name}
                      </p>

                      <p className="text-xs text-gray-400">
                        Team ID: {team.id}
                      </p>
                    </div>
                  </div>
                </td>

                <td className="px-5 py-4 text-sm text-gray-600">
                  {team.members}
                </td>

                <td className="px-5 py-4 text-sm text-gray-600">
                  {team.location}
                </td>

                <td className="px-5 py-4">
                  <span
                    className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${
                      team.status === "Available"
                        ? "bg-green-50 text-green-600"
                        : team.status === "On Mission"
                          ? "bg-blue-50 text-blue-600"
                          : "bg-gray-100 text-gray-500"
                    }`}
                  >
                    <span className="h-1.5 w-1.5 rounded-full bg-current" />
                    {team.status}
                  </span>
                </td>

                <td className="px-5 py-4">
                  <button className="text-xs font-semibold text-red-500 hover:text-red-700">
                    View
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}