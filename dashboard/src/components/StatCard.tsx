interface StatCardProps {
  title: string;
  value: string;
  subtitle: string;
  type: "victim" | "available" | "active";
}

export default function StatCard({
  title,
  value,
  subtitle,
  type,
}: StatCardProps) {
  const styles = {
    victim: {
      box: "bg-red-50",
      icon: "bg-red-100 text-red-600",
      value: "text-red-600",
    },
    available: {
      box: "bg-green-50",
      icon: "bg-green-100 text-green-600",
      value: "text-green-600",
    },
    active: {
      box: "bg-blue-50",
      icon: "bg-blue-100 text-blue-600",
      value: "text-blue-600",
    },
  };

  const style = styles[type];

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500">
            {title}
          </p>

          <p className={`mt-2 text-3xl font-bold ${style.value}`}>
            {value}
          </p>
        </div>

        <div
          className={`flex h-11 w-11 items-center justify-center rounded-xl text-lg ${style.icon}`}
        >
          {type === "victim" && "!"}
          {type === "available" && "✓"}
          {type === "active" && "↗"}
        </div>
      </div>

      <p className="mt-3 text-xs text-gray-500">
        {subtitle}
      </p>
    </div>
  );
}