import { useEffect, useState, useCallback } from 'react'
import { analyticsApi, routeApi } from '../../api'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  LineChart, Line, Cell, ReferenceLine,
} from 'recharts'

const CURRENT_YEAR = new Date().getFullYear()

function classifyMonth(row, maxPax, minPax) {
  if (row.passengerCount === maxPax) return 'peak'
  if (row.passengerCount === minPax) return 'low'
  return 'normal'
}

const barColor = kind =>
  kind === 'peak' ? '#16a34a' : kind === 'low' ? '#dc2626' : '#2563eb'

function MonthlyKpis({ data }) {
  if (!data.length) return null
  const maxPax     = Math.max(...data.map(m => m.passengerCount))
  const minPax     = Math.min(...data.map(m => m.passengerCount))
  const avgPax     = Math.round(data.reduce((s, m) => s + m.passengerCount, 0) / data.length)
  const peakMonth  = data.find(m => m.passengerCount === maxPax)
  const lowMonth   = data.find(m => m.passengerCount === minPax)

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div className="card text-center border-t-4 border-t-green-500">
        <p className="text-xs text-gray-500 mb-1">Peak Month</p>
        <p className="font-bold text-green-700 text-sm">{peakMonth?.monthName} {peakMonth?.year}</p>
        <p className="text-2xl font-extrabold text-gray-800">{maxPax.toLocaleString()}</p>
        <p className="text-xs text-gray-400">passengers</p>
      </div>
      <div className="card text-center border-t-4 border-t-red-500">
        <p className="text-xs text-gray-500 mb-1">Lowest Month</p>
        <p className="font-bold text-red-700 text-sm">{lowMonth?.monthName} {lowMonth?.year}</p>
        <p className="text-2xl font-extrabold text-gray-800">{minPax.toLocaleString()}</p>
        <p className="text-xs text-gray-400">passengers</p>
      </div>
      <div className="card text-center border-t-4 border-t-blue-500">
        <p className="text-xs text-gray-500 mb-1">Monthly Average</p>
        <p className="text-2xl font-extrabold text-gray-800">{avgPax.toLocaleString()}</p>
        <p className="text-xs text-gray-400">passengers / month</p>
      </div>
      <div className="card text-center border-t-4 border-t-purple-500">
        <p className="text-xs text-gray-500 mb-1">Months Tracked</p>
        <p className="text-2xl font-extrabold text-gray-800">{data.length}</p>
        <p className="text-xs text-gray-400">months with data</p>
      </div>
    </div>
  )
}

function TopBottomCards({ data }) {
  if (!data.length) return null
  const maxPax     = Math.max(...data.map(m => m.passengerCount))
  const peakMonths = [...data].sort((a, b) => b.passengerCount - a.passengerCount).slice(0, 3)
  const lowMonths  = [...data].sort((a, b) => a.passengerCount - b.passengerCount).slice(0, 3)

  const BarRow = ({ m, i, color, bg }) => (
    <div className="flex items-center gap-3 py-2 border-b border-gray-50 last:border-0">
      <span className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white flex-shrink-0 ${bg[i]}`}>
        {i + 1}
      </span>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-gray-800">{m.monthName} {m.year}</p>
        <div className="mt-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
          <div className={`h-full ${color} rounded-full`}
            style={{ width: `${maxPax > 0 ? (m.passengerCount / maxPax) * 100 : 0}%` }} />
        </div>
      </div>
      <span className="text-sm font-bold text-gray-700 flex-shrink-0">{m.passengerCount.toLocaleString()}</span>
    </div>
  )

  return (
    <div className="grid md:grid-cols-2 gap-6 mb-6">
      <div className="card">
        <h2 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-green-500 inline-block" /> Top 3 Peak Months
        </h2>
        {peakMonths.map((m, i) => (
          <BarRow key={i} m={m} i={i} color="bg-green-500"
            bg={['bg-green-600', 'bg-green-400', 'bg-green-300']} />
        ))}
      </div>
      <div className="card">
        <h2 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-red-500 inline-block" /> Top 3 Lowest Months
        </h2>
        {lowMonths.map((m, i) => (
          <BarRow key={i} m={m} i={i} color="bg-red-400"
            bg={['bg-red-600', 'bg-red-400', 'bg-red-300']} />
        ))}
      </div>
    </div>
  )
}

function MonthlyChart({ data, title }) {
  const maxPax = data.length > 0 ? Math.max(...data.map(m => m.passengerCount)) : 0
  const minPax = data.length > 0 ? Math.min(...data.map(m => m.passengerCount)) : 0
  const avgPax = data.length > 0
    ? Math.round(data.reduce((s, m) => s + m.passengerCount, 0) / data.length)
    : 0

  const chartData = data.map(m => ({
    name: `${m.monthName.slice(0, 3)} ${m.year}`,
    passengers: m.passengerCount,
    revenue: Number(m.revenue),
    kind: classifyMonth(m, maxPax, minPax),
  }))

  return (
    <div className="card mb-6">
      <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
        <h2 className="text-sm font-semibold text-gray-700">{title}</h2>
        <div className="flex items-center gap-4 text-xs text-gray-500">
          <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-sm bg-green-600 inline-block" /> Peak</span>
          <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-sm bg-red-600 inline-block" /> Lowest</span>
          <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-sm bg-blue-600 inline-block" /> Normal</span>
        </div>
      </div>
      {chartData.length > 0 ? (
        <ResponsiveContainer width="100%" height={240}>
          <BarChart data={chartData} margin={{ left: 0, right: 16 }}>
            <XAxis dataKey="name" tick={{ fontSize: 9 }} angle={-40} textAnchor="end" height={58} />
            <YAxis tick={{ fontSize: 10 }} />
            <Tooltip formatter={v => [v.toLocaleString(), 'Passengers']} />
            <ReferenceLine y={avgPax} stroke="#9ca3af" strokeDasharray="4 3"
              label={{ value: `Avg ${avgPax.toLocaleString()}`, position: 'right', fontSize: 9, fill: '#6b7280' }} />
            <Bar dataKey="passengers" radius={[4, 4, 0, 0]}>
              {chartData.map((e, i) => <Cell key={i} fill={barColor(e.kind)} />)}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      ) : (
        <div className="text-center text-gray-400 py-10 text-sm">No data for this selection</div>
      )}
    </div>
  )
}

function RankedTable({ data }) {
  const maxPax   = data.length > 0 ? Math.max(...data.map(m => m.passengerCount)) : 0
  const minPax   = data.length > 0 ? Math.min(...data.map(m => m.passengerCount)) : 0
  const ranked   = [...data].sort((a, b) => b.passengerCount - a.passengerCount)

  return (
    <div className="card">
      <h2 className="text-sm font-semibold text-gray-700 mb-4">All Months — Ranked by Passengers</h2>
      <div className="table-container">
        <table className="table-base">
          <thead>
            <tr>
              <th className="th">Rank</th><th className="th">Month</th><th className="th">Year</th>
              <th className="th">Passengers</th><th className="th">Bookings</th>
              <th className="th">Revenue</th><th className="th">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {ranked.map((m, i) => {
              const kind = classifyMonth(m, maxPax, minPax)
              return (
                <tr key={i} className={kind === 'peak' ? 'bg-green-50' : kind === 'low' ? 'bg-red-50' : 'hover:bg-gray-50'}>
                  <td className="td font-mono text-xs text-gray-400">{i + 1}</td>
                  <td className="td font-medium">{m.monthName}</td>
                  <td className="td">{m.year}</td>
                  <td className="td font-bold">{m.passengerCount.toLocaleString()}</td>
                  <td className="td">{m.bookingCount}</td>
                  <td className="td text-green-700 font-medium">₹{Number(m.revenue).toLocaleString()}</td>
                  <td className="td">
                    {kind === 'peak' && <span className="px-2 py-0.5 rounded-full text-xs font-bold bg-green-100 text-green-700">Peak</span>}
                    {kind === 'low'  && <span className="px-2 py-0.5 rounded-full text-xs font-bold bg-red-100 text-red-700">Lowest</span>}
                    {kind === 'normal' && <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-500">Normal</span>}
                  </td>
                </tr>
              )
            })}
            {data.length === 0 && (
              <tr><td colSpan={7} className="td text-center text-gray-400 py-8">No data for selected range</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

// ── Main page ──────────────────────────────────────────────────────────────────

export default function MonthlyAnalyticsPage() {
  const [overall,     setOverall]     = useState([])
  const [routeData,   setRouteData]   = useState([])
  const [routes,      setRoutes]      = useState([])
  const [selectedRoute, setSelectedRoute] = useState('all')
  const [loading,     setLoading]     = useState(true)
  const [routeLoading,setRouteLoading]= useState(false)
  const [fromYear,    setFromYear]    = useState(CURRENT_YEAR - 2)
  const [toYear,      setToYear]      = useState(CURRENT_YEAR)

  // Fetch all routes once for the dropdown
  useEffect(() => {
    routeApi.getAll().then(r => setRoutes(r.data.data || r.data || []))
  }, [])

  // Overall monthly data
  const loadOverall = useCallback(() => {
    setLoading(true)
    analyticsApi.monthly(fromYear, toYear)
      .then(r => setOverall(r.data.data || []))
      .finally(() => setLoading(false))
  }, [fromYear, toYear])

  useEffect(() => { loadOverall() }, [loadOverall])

  // Per-route data whenever route selection changes
  useEffect(() => {
    if (selectedRoute === 'all') { setRouteData([]); return }
    setRouteLoading(true)
    analyticsApi.monthlyByRoute(selectedRoute, fromYear, toYear)
      .then(r => setRouteData(r.data.data || []))
      .finally(() => setRouteLoading(false))
  }, [selectedRoute, fromYear, toYear])

  const years = []
  for (let y = 2022; y <= CURRENT_YEAR + 1; y++) years.push(y)

  const selectedRouteName = routes.find(r => String(r.id) === String(selectedRoute))?.name

  return (
    <div>
      {/* ── Header + controls ── */}
      <div className="flex flex-col gap-3 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <h1 className="text-xl font-bold text-gray-800">Monthly Passenger Demand</h1>
            <p className="text-xs text-gray-500 mt-0.5">Track peak and lowest passenger months — overall and per route</p>
          </div>
          <div className="flex items-center gap-3 flex-wrap">
            {[{ label: 'From', val: fromYear, set: setFromYear }, { label: 'To', val: toYear, set: setToYear }].map(f => (
              <div key={f.label} className="flex items-center gap-2">
                <label className="text-xs font-medium text-gray-500">{f.label}</label>
                <select className="input-field text-sm w-24 py-1" value={f.val}
                  onChange={e => f.set(Number(e.target.value))}>
                  {years.map(y => <option key={y}>{y}</option>)}
                </select>
              </div>
            ))}
          </div>
        </div>

        {/* Route selector */}
        <div className="flex items-center gap-3 bg-white border border-gray-100 rounded-xl px-4 py-3 shadow-sm flex-wrap">
          <span className="text-xs font-medium text-gray-500">Filter by route:</span>
          <button
            onClick={() => setSelectedRoute('all')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors
              ${selectedRoute === 'all' ? 'bg-blue-900 text-white border-blue-900' : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50'}`}>
            All Routes (Overall)
          </button>
          {routes.map(r => (
            <button key={r.id}
              onClick={() => setSelectedRoute(String(r.id))}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors
                ${String(selectedRoute) === String(r.id) ? 'bg-blue-900 text-white border-blue-900' : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50'}`}>
              {r.name}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><LoadingSpinner /></div>
      ) : (
        <>
          {/* ── OVERALL section ── */}
          <div className={selectedRoute !== 'all' ? 'opacity-50' : ''}>
            <div className="flex items-center gap-2 mb-3">
              <span className="w-2 h-4 rounded-sm bg-blue-600 inline-block" />
              <h2 className="text-sm font-semibold text-gray-700">Overall — All Routes Combined</h2>
            </div>
            <MonthlyKpis data={overall} />
            <TopBottomCards data={overall} />
            <MonthlyChart data={overall} title="Monthly Passengers — All Routes" />
          </div>

          {/* ── PER-ROUTE section ── */}
          {selectedRoute !== 'all' && (
            <div className="mt-8">
              <div className="flex items-center gap-2 mb-3">
                <span className="w-2 h-4 rounded-sm bg-purple-600 inline-block" />
                <h2 className="text-sm font-semibold text-gray-700">
                  Route Breakdown — <span className="text-purple-700">{selectedRouteName}</span>
                </h2>
              </div>
              {routeLoading ? (
                <div className="flex justify-center py-10"><LoadingSpinner /></div>
              ) : (
                <>
                  <MonthlyKpis data={routeData} />
                  <TopBottomCards data={routeData} />
                  <MonthlyChart data={routeData} title={`Monthly Passengers — ${selectedRouteName}`} />
                  <RankedTable data={routeData} />
                </>
              )}
            </div>
          )}

          {/* ── Ranked table (overall, shown when no route filter) ── */}
          {selectedRoute === 'all' && <RankedTable data={overall} />}
        </>
      )}
    </div>
  )
}
