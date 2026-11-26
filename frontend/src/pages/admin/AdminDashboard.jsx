import { useEffect, useState } from 'react'
import { analyticsApi } from '../../api'
import StatCard from '../../components/admin/StatCard'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  LineChart, Line, CartesianGrid, Area, AreaChart
} from 'recharts'
import { FiUsers, FiMap, FiTruck, FiBookmark, FiDollarSign, FiPercent, FiActivity } from 'react-icons/fi'

const CustomTooltip = ({ active, payload, label, prefix = '' }) => {
  if (!active || !payload?.length) return null
  return (
    <div className="bg-navy-900 text-white text-xs rounded-xl px-3 py-2.5 shadow-modal">
      <p className="font-semibold text-navy-200 mb-1">{label}</p>
      {payload.map((p, i) => (
        <div key={i} className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full" style={{ background: p.color }} />
          <span>{prefix}{Number(p.value).toLocaleString('en-IN')}</span>
        </div>
      ))}
    </div>
  )
}

export default function AdminDashboard() {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    analyticsApi.dashboard()
      .then(r => setStats(r.data.data))
      .catch(() => setError('Failed to load dashboard data'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingSpinner />
  if (error) return (
    <div className="flex items-center justify-center py-24">
      <div className="card text-center max-w-sm">
        <FiActivity size={32} className="text-red-400 mx-auto mb-3" />
        <p className="text-navy-700 font-semibold">{error}</p>
      </div>
    </div>
  )

  const monthlyData = stats?.monthlyDemand?.map(m => ({
    name: `${m.monthName.slice(0, 3)} '${String(m.year).slice(2)}`,
    passengers: m.passengerCount,
    revenue: Number(m.revenue)
  })) || []

  return (
    <div className="animate-slide-up">
      {/* Page header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">Admin Dashboard</h1>
          <p className="page-subtitle">Live overview of Karnataka Travels network</p>
        </div>
        <div className="flex items-center gap-2 text-xs text-navy-500 bg-white border border-navy-100 px-3 py-2 rounded-xl shadow-card">
          <span className="live-dot" />
          <span>Refreshed just now</span>
        </div>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 mb-8">
        <StatCard title="Total Passengers"    value={stats?.totalPassengers ?? 0}                                          icon={FiUsers}    color="blue" />
        <StatCard title="Active Routes"       value={stats?.totalActiveRoutes ?? 0}                                        icon={FiMap}      color="green" />
        <StatCard title="Total Buses"         value={stats?.totalBuses ?? 0}                                               icon={FiTruck}    color="purple" />
        <StatCard title="Total Bookings"      value={stats?.totalBookings ?? 0}                                            icon={FiBookmark} color="indigo" />
        <StatCard title="Revenue (YTD)"       value={`₹${Number(stats?.totalRevenue ?? 0).toLocaleString('en-IN')}`}      icon={FiDollarSign} color="yellow" />
        <StatCard title="Avg Occupancy"       value={`${stats?.averageOccupancy ?? 0}%`}                                   icon={FiPercent}  color="red" />
        <StatCard title="Transported (YTD)"   value={(stats?.totalPassengersTransported ?? 0).toLocaleString('en-IN')}     icon={FiUsers}    color="teal" subtitle="Passengers" />
      </div>

      {/* Charts row */}
      <div className="grid md:grid-cols-2 gap-5 mb-6">
        <div className="card">
          <h2 className="section-title">Monthly Passenger Demand</h2>
          {monthlyData.length > 0 ? (
            <ResponsiveContainer width="100%" height={230}>
              <BarChart data={monthlyData} barCategoryGap="35%">
                <CartesianGrid strokeDasharray="3 3" stroke="#e8edf5" vertical={false} />
                <XAxis dataKey="name" tick={{ fontSize: 10, fill: '#64748b' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 10, fill: '#64748b' }} axisLine={false} tickLine={false} />
                <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(12,25,41,0.04)' }} />
                <Bar dataKey="passengers" fill="url(#barGrad)" radius={[6, 6, 0, 0]} />
                <defs>
                  <linearGradient id="barGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#2b4a7a" />
                    <stop offset="100%" stopColor="#1a2d4a" />
                  </linearGradient>
                </defs>
              </BarChart>
            </ResponsiveContainer>
          ) : <div className="text-center text-navy-300 py-10 text-sm">No monthly data yet</div>}
        </div>

        <div className="card">
          <h2 className="section-title">Monthly Revenue</h2>
          {monthlyData.length > 0 ? (
            <ResponsiveContainer width="100%" height={230}>
              <AreaChart data={monthlyData}>
                <defs>
                  <linearGradient id="revGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#d97706" stopOpacity={0.2} />
                    <stop offset="95%" stopColor="#d97706" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e8edf5" vertical={false} />
                <XAxis dataKey="name" tick={{ fontSize: 10, fill: '#64748b' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 10, fill: '#64748b' }} axisLine={false} tickLine={false} />
                <Tooltip content={<CustomTooltip prefix="₹" />} />
                <Area type="monotone" dataKey="revenue" stroke="#d97706" strokeWidth={2.5} fill="url(#revGrad)" dot={false} />
              </AreaChart>
            </ResponsiveContainer>
          ) : <div className="text-center text-navy-300 py-10 text-sm">No revenue data yet</div>}
        </div>
      </div>

      {/* Busiest stations table */}
      <div className="card">
        <h2 className="section-title">Busiest Stations</h2>
        <div className="table-container">
          <table className="table-base">
            <thead>
              <tr>
                <th className="th">#</th>
                <th className="th">Station</th>
                <th className="th">Boarding Count</th>
                <th className="th">Activity</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-navy-50">
              {stats?.busiestStations?.length > 0 ? stats.busiestStations.map((s, idx) => {
                const max = stats.busiestStations[0].boardingCount
                const pct = Math.round((s.boardingCount / max) * 100)
                return (
                  <tr key={s.stationId} className="tr-hover">
                    <td className="td text-navy-400 font-semibold w-10">{idx + 1}</td>
                    <td className="td font-semibold text-navy-800">{s.stationName}</td>
                    <td className="td text-navy-700">{s.boardingCount.toLocaleString('en-IN')}</td>
                    <td className="td">
                      <div className="flex items-center gap-2">
                        <div className="flex-1 h-1.5 bg-navy-100 rounded-full overflow-hidden">
                          <div className="h-full rounded-full bg-gold-gradient" style={{ width: `${pct}%` }} />
                        </div>
                        <span className="text-xs text-navy-400 w-8 text-right">{pct}%</span>
                      </div>
                    </td>
                  </tr>
                )
              }) : (
                <tr><td colSpan={4} className="td text-center text-navy-300 py-8">No station data yet</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
