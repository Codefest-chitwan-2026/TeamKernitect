interface TopbarProps {
  title: string;
}

export default function Topbar({ title }: TopbarProps) {
  return (
    <header className="flex h-20 shrink-0 items-center justify-between border-b border-gray-200 bg-white px-8">
      <div>
        <h1 className="text-xl font-bold text-gray-900">
          {title}
        </h1>

        <p className="mt-0.5 text-xs text-gray-500">
          Emergency response management system
        </p>
      </div>

      <div className="flex items-center gap-5">
        

        {/* Divider */}
        <div className="h-8 w-px bg-gray-200" />

        {/* Profile */}
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-red-100 font-bold text-red-600">
            A
          </div>

          <div className="hidden sm:block">
            <p className="text-sm font-semibold text-gray-800">
              Administrator
            </p>

            <p className="text-[11px] text-gray-500">
              Emergency Operations
            </p>
          </div>

          <span className="text-xs text-gray-400">⌄</span>
        </div>
      </div>
    </header>
  );
}