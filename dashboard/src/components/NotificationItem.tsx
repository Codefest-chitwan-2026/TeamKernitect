interface NotificationItemProps {
  type: "emergency" | "team" | "success";
  title: string;
  description: string;
  time: string;
}

export default function NotificationItem({
  type,
  title,
  description,
  time,
}: NotificationItemProps) {
  const styles = {
    emergency: "bg-red-50 text-red-600",
    team: "bg-blue-50 text-blue-600",
    success: "bg-green-50 text-green-600",
  };

  return (
    <div className="flex gap-4 border-b border-gray-100 px-5 py-5 last:border-0">
      <div
        className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-sm font-bold ${styles[type]}`}
      >
        {type === "emergency" && "!"}
        {type === "team" && "+"}
        {type === "success" && "✓"}
      </div>

      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-3">
          <p className="text-sm font-semibold text-gray-800">
            {title}
          </p>

          <span className="shrink-0 text-[11px] text-gray-400">
            {time}
          </span>
        </div>

        <p className="mt-1 text-xs leading-5 text-gray-500">
          {description}
        </p>
      </div>
    </div>
  );
}