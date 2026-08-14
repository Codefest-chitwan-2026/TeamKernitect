import NotificationItem from "../components/NotificationItem";
import StatCard from "../components/StatCard";
import EmergencyMap from "../components/EmergencyMap";

export default function Dashboard() {
  return (
    <div className="space-y-6">
      {/* Welcome */}
      <div>
 
        <p className="text-sm font-bold  text-gray-500">
          Monitor emergency situations and coordinate rescue operations.
        </p>
      </div>

      {/* Statistics */}
      <div className="grid grid-cols-1 gap-5 md:grid-cols-3">
        <StatCard
          title="Active Victims"
          value="05"
          subtitle="People currently awaiting assistance"
          type="victim"
        />

        <StatCard
          title="Available Rescue Teams"
          value="05"
          subtitle="Teams ready for deployment"
          type="available"
        />

        <StatCard
          title="Teams On Mission"
          value="03"
          subtitle="Teams currently responding"
          type="active"
        />
      </div>

      {/* Map */}
      <EmergencyMap />

      {/* Bottom section */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Response overview */}
        <div className="rounded-xl border border-gray-200 bg-white shadow-sm">
          <div className="border-b border-gray-100 px-5 py-4">
            <h2 className="font-semibold text-gray-900">
              Response Overview
            </h2>

            <p className="mt-1 text-xs text-gray-500">
              Current emergency network activity
            </p>
          </div>

          <div className="space-y-5 p-5">
            <div>
              <div className="mb-2 flex justify-between">
                <span className="text-xs font-medium text-gray-600">
                  Active incidents
                </span>

                <span className="text-xs font-bold text-red-600">
                  5
                </span>
              </div>

              <div className="h-2 overflow-hidden rounded-full bg-gray-100">
                <div className="h-full w-[55%] rounded-full bg-red-500" />
              </div>
            </div>

            <div>
              <div className="mb-2 flex justify-between">
                <span className="text-xs font-medium text-gray-600">
                  Teams available
                </span>

                <span className="text-xs font-bold text-green-600">
                  5
                </span>
              </div>

              <div className="h-2 overflow-hidden rounded-full bg-gray-100">
                <div className="h-full w-[70%] rounded-full bg-green-500" />
              </div>
            </div>

            <div>
              <div className="mb-2 flex justify-between">
                <span className="text-xs font-medium text-gray-600">
                  Active missions
                </span>

                <span className="text-xs font-bold text-blue-600">
                  3
                </span>
              </div>

              <div className="h-2 overflow-hidden rounded-full bg-gray-100">
                <div className="h-full w-[40%] rounded-full bg-blue-500" />
              </div>
            </div>
          </div>
        </div>

        {/* Recent notifications */}
        <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
          <div className="border-b border-gray-100 px-5 py-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="font-semibold text-gray-900">
                  Recent Activity
                </h2>

                <p className="mt-1 text-xs text-gray-500">
                  Latest system updates
                </p>
              </div>

              <button className="text-xs font-semibold text-red-500 hover:text-red-700">
                View all
              </button>
            </div>
          </div>

          <NotificationItem
            type="emergency"
            title="New SOS received"
            description="Emergency reported in Kathmandu."
            time="2 min"
          />

          <NotificationItem
            type="team"
            title="Team K-01 available"
            description="Kathmandu Response Unit is ready for deployment."
            time="8 min"
          />

          <NotificationItem
            type="success"
            title="Rescue completed"
            description="Emergency location has been successfully resolved."
            time="18 min"
          />
        </div>
      </div>
    </div>
  );
}