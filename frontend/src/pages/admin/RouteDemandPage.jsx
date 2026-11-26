import { useEffect, useState, useCallback } from 'react'
import { analyticsApi } from '../../api'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import StatusBadge from '../../components/common/StatusBadge'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell,
  LineChart, Line, Legend, ReferenceLine,
} from 'recharts'

// ── Constants ────────────────────────────────────────────────────────────────

const CURRENT_YEAR = new Date().getFullYear()

const SORT_OPTIONS = [
  { value: 'passengers_desc', label: 'Busiest Routes',    icon: '🔥' },
  { value: 'passengers_asc',  label: 'Least Traveled',    icon: '🪶' },
  { value: 'occupancy_desc',  label: 'Highest Occupancy', icon: '📈' },
  { value: 'occupancy_asc',   label: 'Lowest Occupancy',  icon: '📉' },
]

const CHART_COLORS = {
  passengers_desc: '#2563eb',
  passengers_asc:  '#9ca3af',
  occupancy_desc:  '#16a34a',
  occupancy_asc:   '#f59e0b',
}

const TIER_COLOR  = { HIGH: '#16a34a', NORMAL: '#2563eb', LOW: '#ef4444' }
const TIER_BG     = { HIGH: 'bg-green-100 text-green-700', NORMAL: 'bg-blue-100 text-blue-700', LOW: 'bg-red-100 text-red-700' }
const TIER_LABEL  = { HIGH: 'Thick Route', NORMAL: 'Normal', LOW: 'Thin Route' }

const THICK_COLORS  = ['#16a34a', '#15803d', '#166534']
const THIN_COLORS   = ['#ef4444', '#dc2626', '#b91c1c']

const MONTH_ORDER = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']

const TABS = [
  { key: 'sorted',   label: 'Sorted View' },
  { key: 'overview', label: 'Demand Overview' },
  { key: 'trends',   label: 'Route Trends' },
]

// ── Date helpers ─────────────────────────────────────────────────────────────

function toDateInput(d) { return d.toISOString().slice(0, 10) }
function dayStart(d) { const x = new Date(d); x.setHours(0,0,0,0); return x }
function dayEnd(d)   { const x = new Date(d); x.setHours(23,59,59,999); return x }
function toISO(d)    { return d.toISOString().slice(0, 19) }

const today = new Date()
const PERIODS = [
  { label: 'Today',       key: 'today' },
  { label: 'Yesterday',   key: 'yesterday' },
  { label: 'Last 7 Days', key: 'last7' },
  { label: 'Last 30 Days',key: 'last30' },
  { label: 'Custom',      key: 'custom' },
]

function periodRange(key) {
  const now = new Date()
  switch (key) {
    case 'today':     return [dayStart(now), dayEnd(now)]
    case 'yesterday': { const y = new Date(now); y.setDate(y.getDate()-1); return [dayStart(y), dayEnd(y)] }
    case 'last7':     { const s = new Date(now); s.setDate(s.getDate()-6); return [dayStart(s), dayEnd(now)] }
    case 'last30':    { const s = new Date(now); s.setDate(s.getDate()-29); return [dayStart(s), dayEnd(now)] }
    default: return [null, null]
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function demandTier(rank, total) {
  const pct = rank / total
  if (pct <= 0.3) return 'HIGH'
  if (pct <= 0.7) return 'NORMAL'
  return 'LOW'
}

function buildTrendsChart(trendsData, routeIds) {
  const monthSet = new Set()
  routeIds.forEach(id => {
    ;(trendsData[id] || []).forEach(m => monthSet.add(`${m.monthName.slice(0,3)} ${m.year}`))
  })
  const months = [...monthSet].sort((a, b) => {
    const [ma, ya] = a.split(' '); const [mb, yb] = b.split(' ')
    if (ya !== yb) return Number(ya) - Number(yb)
    return MONTH_ORDER.indexOf(ma) - MONTH_ORDER.indexOf(mb)
  })
  return months.map(month => {
    const row = { month }
    routeIds.forEach(id => {
      const mData = (trendsData[id] || []).find(m => `${m.monthName.slice(0,3)} ${m.year}` === month)
      row[`r${id}`] = mData?.passengerCount ?? null
    })
    return row
  })
}

// ── Sub-components ────────────────────────────────────────────────────────────

function TierLegend() {
  return (
    <div className="flex items-center gap-4 text-xs text-gray-500 flex-wrap">
      {Object.entries(TIER_LABEL).map(([tier, label]) => (
        <span key={tier} className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded-sm inline-block" style={{ background: TIER_COLOR[tier] }} />
          {label}
        </span>
      ))}
    </div>
  )
}

function DemandOverviewChart({ data, loading }) {
  if (loading) return <div className="flex justify-center py-16"><LoadingSpinner /></div>
  if (!data.length) return <div className="text-center text-gray-400 py-10">No data available</div>

  const sorted = [...data].sort((a, b) => b.passengerCount - a.passengerCount)
  const total  = sorted.length
  const chartData = sorted.map((r, i) => ({
    name:  r.routeName.length > 24 ? r.routeName.slice(0,24) + '…' : r.routeName,
    value: r.passengerCount,
    tier:  demandTier(i + 1, total),
    rank:  i + 1,
  }))

  const highCount   = chartData.filter(r => r.tier === 'HIGH').length
  const normalCount = chartData.filter(r => r.tier === 'NORMAL').length
  const lowCount    = chartData.filter(r => r.tier === 'LOW').length

  return (
    <div className="space-y-6">
      {/* Summary badges */}
      <div className="flex flex-wrap gap-3">
        <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-green-50 border border-green-200">
          <span className="w-2.5 h-2.5 rounded-full bg-green-600 inline-block" />
          <span className="text-sm font-semibold text-green-700">{highCount} Thick Routes</span>
          <span className="text-xs text-green-500">(high demand)</span>
        </div>
        <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-blue-50 border border-blue-200">
          <span className="w-2.5 h-2.5 rounded-full bg-blue-600 inline-block" />
          <span className="text-sm font-semibold text-blue-700">{normalCount} Normal Routes</span>
        </div>
        <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-red-50 border border-red-200">
          <span className="w-2.5 h-2.5 rounded-full bg-red-500 inline-block" />
          <span className="text-sm font-semibold text-red-700">{lowCount} Thin Routes</span>
          <span className="text-xs text-red-400">(low demand)</span>
        </div>
      </div>

      {/* Main chart */}
      <div className="card">
        <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
          <h2 className="text-sm font-semibold text-gray-700">All Routes — Demand Tier (Top 30% = Thick, Bottom 30% = Thin)</h2>
          <TierLegend />
        </div>
        <ResponsiveContainer width="100%" height={Math.max(280, chartData.length * 28)}>
          <BarChart data={chartData} layout="vertical" margin={{ left: 8, right: 40 }}>
            <XAxis type="number" tick={{ fontSize: 10 }} />
            <YAxis dataKey="name" type="category" tick={{ fontSize: 9 }} width={160} />
            <Tooltip
              formatter={(val, _, props) => [
                `${val.toLocaleString()} passengers`,
                TIER_LABEL[props.payload.tier],
              ]}
              labelStyle={{ fontWeight: 600 }}
            />
            <Bar dataKey="value" radius={[0, 4, 4, 0]} label={{ position: 'right', fontSize: 9, fill: '#6b7280', formatter: v => v.toLocaleString() }}>
              {chartData.map((e, i) => (
                <Cell key={i} fill={TIER_COLOR[e.tier]} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Table */}
      <div className="card">
        <h2 className="text-sm font-semibold text-gray-700 mb-4">Route Classification Table</h2>
        <div className="table-container">
          <table className="table-base">
            <thead>
              <tr>
                <th className="th">#</th>
                <th className="th">Route</th>
                <th className="th">Passengers</th>
                <th className="th">Trips</th>
                <th className="th">Avg Occupancy</th>
                <th className="th">Demand Tier</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {chartData.map((r, i) => (
                <tr key={i} className={
                  r.tier === 'HIGH' ? 'bg-green-50 hover:bg-green-100' :
                  r.tier === 'LOW'  ? 'bg-red-50 hover:bg-red-100' :
                  'hover:bg-gray-50'
                }>
                  <td className="td text-xs text-gray-400">{r.rank}</td>
                  <td className="td font-medium text-xs">{r.name}</td>
                  <td className="td font-bold">{sorted[i].passengerCount.toLocaleString()}</td>
                  <td className="td">{sorted[i].tripCount}</td>
                  <td className="td">
                    <span className={`font-semibold ${sorted[i].averageOccupancy >= 80 ? 'text-red-600' : sorted[i].averageOccupancy < 40 ? 'text-yellow-600' : 'text-green-600'}`}>
                      {sorted[i].averageOccupancy}%
                    </span>
                  </td>
                  <td className="td">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-bold ${TIER_BG[r.tier]}`}>
                      {TIER_LABEL[r.tier]}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

function RouteTrendsChart({ data, loading }) {
  if (loading) return <div className="flex justify-center py-16"><LoadingSpinner /></div>

  const sorted   = [...data].sort((a, b) => b.passengerCount - a.passengerCount)
  const topRoutes    = sorted.slice(0, 3)
  const bottomRoutes = sorted.slice(-3).reverse()

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-4 mb-2">
        <div className="rounded-xl border border-green-200 bg-green-50 p-3">
          <p className="text-xs font-semibold text-green-700 mb-2">Thick Routes (Top 3)</p>
          {topRoutes.map((r, i) => (
            <div key={r.routeId} className="flex items-center gap-2 text-xs py-1">
              <span className="w-3 h-0.5 inline-block rounded" style={{ background: THICK_COLORS[i] }} />
              <span className="text-gray-700">{r.routeName}</span>
              <span className="ml-auto font-bold text-green-700">{r.passengerCount.toLocaleString()}</span>
            </div>
          ))}
        </div>
        <div className="rounded-xl border border-red-200 bg-red-50 p-3">
          <p className="text-xs font-semibold text-red-700 mb-2">Thin Routes (Bottom 3)</p>
          {bottomRoutes.map((r, i) => (
            <div key={r.routeId} className="flex items-center gap-2 text-xs py-1">
              <span className="w-3 h-0.5 inline-block rounded border-t-2 border-dashed" style={{ borderColor: THIN_COLORS[i] }} />
              <span className="text-gray-700">{r.routeName}</span>
              <span className="ml-auto font-bold text-red-600">{r.passengerCount.toLocaleString()}</span>
            </div>
          ))}
        </div>
      </div>
      <TrendsLineChart topRoutes={topRoutes} bottomRoutes={bottomRoutes} />
    </div>
  )
}

function TrendsLineChart({ topRoutes, bottomRoutes }) {
  const [trendsData, setTrendsData] = useState({})
  const [loading, setLoading]       = useState(false)

  const allRoutes = [...topRoutes, ...bottomRoutes]
  const routeIds  = allRoutes.map(r => r.routeId)

  useEffect(() => {
    if (!routeIds.length) return
    setLoading(true)
    Promise.all(
      routeIds.map(id =>
        analyticsApi.monthlyByRoute(id, CURRENT_YEAR - 1, CURRENT_YEAR)
          .then(r => ({ id, months: r.data.data || [] }))
          .catch(() => ({ id, months: [] }))
      )
    ).then(results => {
      const map = {}
      results.forEach(({ id, months }) => { map[id] = months })
      setTrendsData(map)
    }).finally(() => setLoading(false))
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [routeIds.join(',')])

  if (loading) return <div className="flex justify-center py-16"><LoadingSpinner /></div>

  const chartData = buildTrendsChart(trendsData, routeIds)

  if (!chartData.length) return (
    <div className="card text-center text-gray-400 py-10 text-sm">
      No monthly trend data available for the current year range
    </div>
  )

  return (
    <div className="card">
      <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
        <h2 className="text-sm font-semibold text-gray-700">Monthly Passenger Trends — Thick vs Thin Routes</h2>
        <span className="text-xs text-gray-400">{CURRENT_YEAR - 1} – {CURRENT_YEAR}</span>
      </div>
      <ResponsiveContainer width="100%" height={320}>
        <LineChart data={chartData} margin={{ left: 0, right: 16 }}>
          <XAxis dataKey="month" tick={{ fontSize: 9 }} angle={-35} textAnchor="end" height={52} />
          <YAxis tick={{ fontSize: 10 }} />
          <Tooltip formatter={(val, key) => {
            const id = Number(key.replace('r', ''))
            const route = [...topRoutes, ...bottomRoutes].find(r => r.routeId === id)
            return [val?.toLocaleString() ?? '-', route?.routeName ?? key]
          }} />
          <Legend
            formatter={(key) => {
              const id = Number(key.replace('r', ''))
              const route = [...topRoutes, ...bottomRoutes].find(r => r.routeId === id)
              return <span style={{ fontSize: 10 }}>{route?.routeName ?? key}</span>
            }}
          />
          {topRoutes.map((r, i) => (
            <Line
              key={r.routeId}
              type="monotone"
              dataKey={`r${r.routeId}`}
              stroke={THICK_COLORS[i]}
              strokeWidth={2.5}
              dot={{ r: 3 }}
              connectNulls
            />
          ))}
          {bottomRoutes.map((r, i) => (
            <Line
              key={r.routeId}
              type="monotone"
              dataKey={`r${r.routeId}`}
              stroke={THIN_COLORS[i]}
              strokeWidth={2}
              strokeDasharray="5 3"
              dot={{ r: 3 }}
              connectNulls
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
      <p className="text-xs text-gray-400 mt-2 text-center">
        Solid lines = thick (high demand) routes &nbsp;·&nbsp; Dashed lines = thin (low demand) routes
      </p>
    </div>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────

export default function RouteDemandPage() {
  const [data,     setData]     = useState([])
  const [sortBy,   setSortBy]   = useState('passengers_desc')
  const [loading,  setLoading]  = useState(true)
  const [period,   setPeriod]   = useState('last30')
  const [tab,      setTab]      = useState('sorted')
  const [customFrom, setCustomFrom] = useState(toDateInput(new Date(today.getFullYear(), today.getMonth(), 1)))
  const [customTo,   setCustomTo]   = useState(toDateInput(today))

  const getRange = useCallback(() => {
    if (period === 'custom') {
      const f = customFrom ? dayStart(new Date(customFrom)) : null
      const t = customTo   ? dayEnd(new Date(customTo))     : null
      return [f ? toISO(f) : null, t ? toISO(t) : null]
    }
    const [f, t] = periodRange(period)
    return [f ? toISO(f) : null, t ? toISO(t) : null]
  }, [period, customFrom, customTo])

  const load = useCallback((sort, range) => {
    setLoading(true)
    const [from, to] = range
    analyticsApi.routeDemand(from, to, sort)
      .then(r => setData(r.data.data || []))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load(sortBy, getRange()) }, [sortBy, load, getRange])

  const activeSort = SORT_OPTIONS.find(o => o.value === sortBy)
  const chartField = sortBy.startsWith('occupancy') ? 'averageOccupancy' : 'passengerCount'
  const chartLabel = sortBy.startsWith('occupancy') ? 'Occupancy %' : 'Passengers'
  const barColor   = CHART_COLORS[sortBy]
  const chartData  = data.slice(0, 10).map(r => ({
    name:  r.routeName.length > 22 ? r.routeName.slice(0, 22) + '…' : r.routeName,
    value: r[chartField],
  }))

  return (
    <div>
      {/* ── Header ── */}
      <div className="flex flex-col gap-4 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <h1 className="text-xl font-bold text-gray-800">Route Demand Analysis</h1>
        </div>

        {/* Tabs */}
        <div className="flex gap-1 bg-gray-100 rounded-xl p-1 w-fit">
          {TABS.map(t => (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                tab === t.key ? 'bg-white text-blue-700 shadow-sm' : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {/* Controls — sort only shown in sorted view */}
        {tab === 'sorted' && (
          <div className="flex flex-wrap gap-2">
            {SORT_OPTIONS.map(opt => (
              <button
                key={opt.value}
                onClick={() => setSortBy(opt.value)}
                className={`px-3 py-1.5 rounded-full text-xs font-medium border transition-colors
                  ${sortBy === opt.value
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'bg-white text-gray-600 border-gray-300 hover:border-blue-400'}`}
              >
                {opt.icon} {opt.label}
              </button>
            ))}
          </div>
        )}

        {/* Period picker */}
        <div className="flex flex-wrap items-center gap-2">
          {PERIODS.map(p => (
            <button
              key={p.key}
              onClick={() => setPeriod(p.key)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors
                ${period === p.key
                  ? 'bg-blue-900 text-white border-blue-900'
                  : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50'}`}
            >
              {p.label}
            </button>
          ))}
          {period === 'custom' && (
            <div className="flex items-center gap-2 ml-1">
              <input type="date" value={customFrom} max={customTo}
                onChange={e => setCustomFrom(e.target.value)}
                className="border border-gray-300 rounded-lg px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-blue-500" />
              <span className="text-gray-400 text-xs">to</span>
              <input type="date" value={customTo} min={customFrom} max={toDateInput(new Date())}
                onChange={e => setCustomTo(e.target.value)}
                className="border border-gray-300 rounded-lg px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-blue-500" />
            </div>
          )}
        </div>
      </div>

      {/* ── Sorted View ── */}
      {tab === 'sorted' && (
        <>
          <div className="card mb-6">
            <h2 className="text-sm font-semibold text-gray-600 mb-4">
              Top 10 Routes — {activeSort?.label} ({chartLabel})
            </h2>
            {loading ? <div className="flex justify-center py-10"><LoadingSpinner /></div>
              : chartData.length > 0 ? (
                <ResponsiveContainer width="100%" height={280}>
                  <BarChart data={chartData} layout="vertical" margin={{ left: 8, right: 16 }}>
                    <XAxis type="number" tick={{ fontSize: 11 }} />
                    <YAxis dataKey="name" type="category" tick={{ fontSize: 10 }} width={150} />
                    <Tooltip
                      formatter={(val) => [
                        sortBy.startsWith('occupancy') ? `${val}%` : val,
                        chartLabel,
                      ]}
                    />
                    <Bar dataKey="value" radius={[0, 4, 4, 0]}>
                      {chartData.map((_, i) => <Cell key={i} fill={barColor} />)}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              ) : <div className="text-center text-gray-400 py-8">No data available</div>}
          </div>

          <div className="table-container">
            <table className="table-base">
              <thead>
                <tr>
                  <th className="th">#</th>
                  <th className="th">Route</th>
                  <th className="th">Passengers</th>
                  <th className="th">Trips</th>
                  <th className="th">Revenue</th>
                  <th className="th">Avg Occupancy</th>
                  <th className="th">Category</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {loading ? (
                  <tr><td colSpan={7} className="td text-center py-8"><LoadingSpinner /></td></tr>
                ) : data.length === 0 ? (
                  <tr><td colSpan={7} className="td text-center text-gray-400">No data</td></tr>
                ) : data.map((r, i) => (
                  <tr key={r.routeId} className="hover:bg-gray-50">
                    <td className="td text-gray-400 text-xs">{i + 1}</td>
                    <td className="td font-medium text-xs">{r.routeName}</td>
                    <td className="td">{r.passengerCount.toLocaleString()}</td>
                    <td className="td">{r.tripCount}</td>
                    <td className="td text-xs">&#8377;{Number(r.revenue).toLocaleString()}</td>
                    <td className="td">
                      <span className={`font-semibold ${r.averageOccupancy >= 80 ? 'text-red-600' : r.averageOccupancy < 40 ? 'text-yellow-600' : 'text-green-600'}`}>
                        {r.averageOccupancy}%
                      </span>
                    </td>
                    <td className="td"><StatusBadge status={r.utilizationCategory} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {/* ── Demand Overview ── */}
      {tab === 'overview' && (
        <DemandOverviewChart data={data} loading={loading} />
      )}

      {/* ── Route Trends ── */}
      {tab === 'trends' && (
        loading
          ? <div className="flex justify-center py-16"><LoadingSpinner /></div>
          : data.length < 2
            ? <div className="text-center text-gray-400 py-10">Not enough route data to compare trends</div>
            : <RouteTrendsChart data={data} loading={loading} />
      )}
    </div>
  )
}
