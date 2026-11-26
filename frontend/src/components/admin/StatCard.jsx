const colorMap = {
  blue:   { bg: 'bg-navy-100',   text: 'text-navy-600',   bar: '#2b4a7a' },
  green:  { bg: 'bg-emerald-100', text: 'text-emerald-600', bar: '#059669' },
  yellow: { bg: 'bg-gold-100',   text: 'text-gold-600',   bar: '#d97706' },
  red:    { bg: 'bg-red-100',    text: 'text-red-600',    bar: '#dc2626' },
  purple: { bg: 'bg-purple-100', text: 'text-purple-600', bar: '#7c3aed' },
  indigo: { bg: 'bg-indigo-100', text: 'text-indigo-600', bar: '#4338ca' },
  teal:   { bg: 'bg-teal-100',   text: 'text-teal-600',   bar: '#0d9488' },
}

export default function StatCard({ title, value, icon: Icon, color = 'blue', subtitle, trend }) {
  const c = colorMap[color] || colorMap.blue
  return (
    <div className="stat-card group relative overflow-hidden">
      {/* Left accent bar */}
      <div className="absolute left-0 top-4 bottom-4 w-[3px] rounded-r-full opacity-70"
        style={{ background: c.bar }} />

      <div className={`p-3 rounded-2xl ${c.bg} flex-shrink-0 group-hover:scale-110 transition-transform`}>
        <Icon size={22} className={c.text} />
      </div>

      <div className="flex-1 min-w-0">
        <p className="text-xs font-semibold text-navy-400 uppercase tracking-wider mb-0.5">{title}</p>
        <p className="text-2xl font-extrabold text-navy-900 leading-none tracking-tight">{value}</p>
        {subtitle && <p className="text-xs text-navy-400 mt-0.5">{subtitle}</p>}
        {trend && (
          <div className={`text-xs font-semibold mt-1 ${trend > 0 ? 'text-emerald-600' : 'text-red-500'}`}>
            {trend > 0 ? '▲' : '▼'} {Math.abs(trend)}% vs last month
          </div>
        )}
      </div>
    </div>
  )
}
