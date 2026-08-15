interface SidebarProps {
  activePage: string;
  onNavigate: (page: string) => void;
}

const navigation = [
  {
    id: "dashboard",
    label: "Dashboard",
    icon: "/dashboard.svg",
  },
  {
    id: "map",
    label: "Map",
    icon: "/map.svg",
  },
  {
    id: "teams",
    label: "Team Management",
    icon: "/team.svg",
  },
  {
    id: "notifications",
    label: "Notifications",
    icon: "/notification.svg",
  },
];

export default function Sidebar({
  activePage,
  onNavigate,
}: SidebarProps) {
  return (
    <aside className="flex h-screen w-64 shrink-0 flex-col border-r border-red-100 bg-white">

      {/* Logo */}
      <div className="flex h-28 items-center justify-center px-6 pt-20">
        <img
          src="/SaharaLogo1.svg"
          alt="Sahara"
          className="h-28 w-auto object-contain"
        />
      </div>

      {/* Navigation */}
      <nav className="mt-20 ">
        <p className="mb-3 px-3 text-[11px] font-bold uppercase tracking-[0.15em] text-gray-400">
          Main Menu
        </p>

        <div className="space-y-1">
          {navigation.map((item) => {
            const active = activePage === item.id;

            return (
              <button
                key={item.id}
                onClick={() => onNavigate(item.id)}
                className={`group flex w-full items-center gap-3 px-3 py-3 text-sm font-medium transition-all ${
                  active
                    ? "bg-red-500 text-white shadow-sm shadow-red-200"
                    : "text-gray-600 hover:bg-red-50 hover:text-red-600"
                }`}
              >
                {/* Icon */}
                <img
                  src={item.icon}
                  alt=""
                  className="h-6 w-6 shrink-0 object-contain"
                />

                {/* Label */}
                <span>{item.label}</span>
              </button>
            );
          })}
        </div>
      </nav>

      {/* System Status */}
      <div className="mt-auto border-t border-red-100 p-4">
        <div className="rounded-xl bg-red-50 p-4">
          <p className="text-[10px] font-bold uppercase tracking-wider text-red-500">
            System Status
          </p>

          <div className="mt-2 flex items-center gap-2">
            <span className="h-2.5 w-2.5 rounded-full bg-green-500 shadow-sm shadow-green-200" />

            <span className="text-xs font-medium text-gray-700">
              All systems operational
            </span>
          </div>
        </div>

        <p className="mt-4 text-center text-[10px] text-gray-400">
          SAHARA Emergency Network
        </p>
      </div>
    </aside>
  );
}