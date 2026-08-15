import NotificationItem from "../components/NotificationItem";

export default function Notifications() {
  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-gray-900">
          Notifications
        </h2>

        <p className="mt-1 text-sm text-gray-500">
          Stay updated with emergency and rescue activity.
        </p>
      </div>

      <div className="rounded-xl border border-gray-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-gray-100 px-5 py-4">
          <div>
            <h3 className="font-semibold text-gray-900">
              Recent Notifications
            </h3>

            <p className="mt-1 text-xs text-gray-500">
              Latest system activity
            </p>
          </div>

          <button className="text-xs font-semibold text-red-500 hover:text-red-700">
            Mark all as read
          </button>
        </div>

        <NotificationItem
          type="emergency"
          title="New SOS received"
          description="An emergency has been reported in Kathmandu."
          time="2 min ago"
        />

        <NotificationItem
          type="emergency"
          title="Critical emergency detected"
          description="Multiple victims reported at an emergency location."
          time="6 min ago"
        />

        <NotificationItem
          type="team"
          title="Rescue team available"
          description="Team K-01 has completed its previous assignment."
          time="8 min ago"
        />

        <NotificationItem
          type="success"
          title="Rescue operation completed"
          description="The assigned team has successfully completed the rescue."
          time="18 min ago"
        />
      </div>
    </div>
  );
}