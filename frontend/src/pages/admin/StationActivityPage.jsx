import { useEffect, useState, useCallback } from 'react'
import { analyticsApi } from '../../api'
import StatCard from '../../components/admin/StatCard'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell,
  LineChart, Line, CartesianGrid, Legend, PieChart, Pie,
} from 'recharts'
import {
  FiMapPin, FiUsers, FiTrendingUp, FiAlertCircle, FiMap,
  FiChevronRight, FiChevronDown, FiX, FiActivity,
} from 'react-icons/fi'
import { MapContainer, TileLayer, CircleMarker, Popup } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'

// ── helpers ───────────────────────────────────────────────────────────────────

const DEMAND_COLORS = { HIGH: '#dc2626', MEDIUM: '#f59e0b', LOW: '#22c55e' }
const DEMAND_BG     = { HIGH: 'bg-red-100 text-red-700', MEDIUM: 'bg-yellow-100 text-yellow-700', LOW: 'bg-green-100 text-green-700' }

const PERIOD_OPTIONS = [
  { label: 'Today',          value: 'today' },
  { label: 'Last 7 days',    value: '7d' },
  { label: 'Last 30 days',   value: '30d' },
  { label: 'This month',     value: 'month' },
  { label: 'Last 3 months',  value: '3m' },
  { label: 'This year',      value: 'year' },
  { label: 'Custom',         value: 'custom' },
]

function periodToDates(period) {
  const now  = new Date()
  const fmt  = d => d.toISOString().split('.')[0]
  const from = new Date(now)
  switch (period) {
    case 'today':  from.setHours(0,0,0,0); break
    case '7d':     from.setDate(now.getDate() - 7); break
    case '30d':    from.setDate(now.getDate() - 30); break
    case 'month':  from.setDate(1); from.setHours(0,0,0,0); break
    case '3m':     from.setMonth(now.getMonth() - 3); break
    case 'year':   from.setMonth(0); from.setDate(1); from.setHours(0,0,0,0); break
    default:       from.setFullYear(now.getFullYear() - 1)
  }
  return { from: fmt(from), to: fmt(now) }
}

const TAB_OVERVIEW    = 'overview'
const TAB_BUSIEST     = 'busiest'
const TAB_ROUTES      = 'routes'
const TAB_TRIPS       = 'trips'
const TAB_PEAK        = 'peak'
const TAB_TRENDS      = 'trends'
const TAB_MAP         = 'map'
const TAB_RECS        = 'recommendations'

const TABS = [
  { id: TAB_OVERVIEW, label: 'Overview',          icon: FiActivity },
  { id: TAB_BUSIEST,  label: 'Station Rankings',  icon: FiTrendingUp },
  { id: TAB_PEAK,     label: 'Peak Hours',         icon: FiAlertCircle },
  { id: TAB_TRENDS,   label: 'Boarding Trends',    icon: FiTrendingUp },
  { id: TAB_MAP,      label: 'Station Map',        icon: FiMap },
  { id: TAB_RECS,     label: 'Recommendations',    icon: FiAlertCircle },
]

// ── Main Component ─────────────────────────────────────────────────────────────

export default function StationActivityPage() {
  const [tab,            setTab]            = useState(TAB_OVERVIEW)
  const [period,         setPeriod]         = useState('year')
  const [customFrom,     setCustomFrom]     = useState('')
  const [customTo,       setCustomTo]       = useState('')
  const [overview,       setOverview]       = useState(null)
  const [ranked,         setRanked]         = useState([])
  const [mapStations,    setMapStations]    = useState([])
  const [trends,         setTrends]         = useState([])
  const [recs,           setRecs]           = useState([])
  const [loading,        setLoading]        = useState(true)
  const [selectedStation,setSelectedStation]= useState(null)
  const [routes,         setRoutes]         = useState([])
  const [routesLoading,  setRoutesLoading]  = useState(false)
  const [selectedRoute,  setSelectedRoute]  = useState(null)
  const [trips,          setTrips]          = useState([])
  const [tripsLoading,   setTripsLoading]   = useState(false)
  const [peakHours,      setPeakHours]      = useState([])
  const [peakStation,    setPeakStation]    = useState(null)
  const [peakLoading,    setPeakLoading]    = useState(false)

  const buildQs = useCallback(() => {
    if (period === 'custom' && customFrom && customTo) {
      return `?from=${customFrom}T00:00:00&to=${customTo}T23:59:59`
    }
    const { from, to } = periodToDates(period)
    return `?from=${from}&to=${to}`
  }, [period, customFrom, customTo])

  const loadOverviewData = useCallback(() => {
    setLoading(true)
    const qs = buildQs()
    Promise.all([
      analyticsApi.stationOverview(qs),
      analyticsApi.stationRanked(qs),
      analyticsApi.stationMap(qs),
      analyticsApi.stationTrends(qs),
      analyticsApi.stationRecommendations(qs),
    ]).then(([ov, rk, mp, tr, rc]) => {
      setOverview(ov.data.data)
      setRanked(rk.data.data || [])
      setMapStations(mp.data.data || [])
      setTrends(tr.data.data || [])
      setRecs(rc.data.data || [])
    }).finally(() => setLoading(false))
  }, [buildQs])

  useEffect(() => { loadOverviewData() }, [loadOverviewData])

  const openStationRoutes = useCallback((station) => {
    setSelectedStation(station)
    setSelectedRoute(null)
    setTrips([])
    setRoutesLoading(true)
    setTab(TAB_ROUTES)
    analyticsApi.stationRoutes(station.stationId, buildQs())
      .then(r => setRoutes(r.data.data || []))
      .finally(() => setRoutesLoading(false))
  }, [buildQs])

  const openRouteTrips = useCallback((route) => {
    if (!selectedStation) return
    setSelectedRoute(route)
    setTripsLoading(true)
    setTab(TAB_TRIPS)
    analyticsApi.stationTripDetails(selectedStation.stationId, route.routeId, buildQs())
      .then(r => setTrips(r.data.data || []))
      .finally(() => setTripsLoading(false))
  }, [selectedStation, buildQs])

  const openPeakHours = useCallback((station) => {
    setPeakStation(station)
    setPeakLoading(true)
    setTab(TAB_PEAK)
    analyticsApi.stationPeakHours(station.stationId, buildQs())
      .then(r => setPeakHours(r.data.data || []))
      .finally(() => setPeakLoading(false))
  }, [buildQs])

  // ── Filters Bar ─────────────────────────────────────────────────────────────
  const FiltersBar = (
    <div className="flex flex-wrap items-center gap-3 mb-6 bg-white p-4 rounded-xl shadow-sm border border-gray-100">
      <span className="text-sm font-medium text-gray-500">Period:</span>
      {PERIOD_OPTIONS.map(opt => (
        <button key={opt.value} onClick={() => setPeriod(opt.value)}
          className={`px-3 py-1.5 rounded-full text-xs font-medium border transition-colors
            ${period === opt.value ? 'bg-blue-600 text-white border-blue-600' : 'bg-white text-gray-600 border-gray-300 hover:border-blue-400'}`}>
          {opt.label}
        </button>
      ))}
      {period === 'custom' && (
        <>
          <input type="date" value={customFrom} onChange={e => setCustomFrom(e.target.value)}
            className="input-field text-xs py-1 px-2 w-36" />
          <span className="text-gray-400 text-xs">to</span>
          <input type="date" value={customTo} onChange={e => setCustomTo(e.target.value)}
            className="input-field text-xs py-1 px-2 w-36" />
          <button onClick={loadOverviewData} className="btn-primary py-1 px-3 text-xs">Apply</button>
        </>
      )}
    </div>
  )

  // ── Tab Bar ─────────────────────────────────────────────────────────────────
  const TabBar = (
    <div className="flex flex-wrap gap-1 mb-6 bg-gray-100 p-1 rounded-xl">
      {TABS.map(t => (
        <button key={t.id} onClick={() => setTab(t.id)}
          className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-medium transition-colors
            ${tab === t.id ? 'bg-white text-blue-700 shadow-sm' : 'text-gray-600 hover:text-gray-900'}`}>
          <t.icon size={14} />{t.label}
        </button>
      ))}
      {selectedStation && (
        <>
          <button onClick={() => setTab(TAB_ROUTES)}
            className={`flex items-center gap-1 px-4 py-2 rounded-lg text-xs font-medium transition-colors
              ${tab === TAB_ROUTES ? 'bg-white text-blue-700 shadow-sm' : 'text-gray-600 hover:text-gray-900'}`}>
            <FiChevronRight size={12}/>{selectedStation.stationName}
          </button>
          {selectedRoute && (
            <button onClick={() => setTab(TAB_TRIPS)}
              className={`flex items-center gap-1 px-4 py-2 rounded-lg text-xs font-medium transition-colors
                ${tab === TAB_TRIPS ? 'bg-white text-blue-700 shadow-sm' : 'text-gray-600 hover:text-gray-900'}`}>
              <FiChevronRight size={12}/>{selectedRoute.routeName}
            </button>
          )}
        </>
      )}
    </div>
  )

  if (loading && tab === TAB_OVERVIEW) return (
    <div className="space-y-4">
      {FiltersBar}
      <div className="flex items-center justify-center py-20"><LoadingSpinner /></div>
    </div>
  )

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-xl font-bold text-gray-800">Bus Station Boarding Analytics</h1>
          <p className="text-xs text-gray-500 mt-0.5">Actual boarding demand from recorded boardings</p>
        </div>
        <button onClick={loadOverviewData} disabled={loading}
          className="btn-secondary py-1.5 px-3 text-xs flex items-center gap-1">
          {loading ? <LoadingSpinner /> : '↻'} Refresh
        </button>
      </div>

      {FiltersBar}

      {/* KPI Cards */}
      {overview && (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-6">
          <StatCard title="Active Stations"       value={overview.totalActiveStations}   icon={FiMapPin}    color="blue" />
          <StatCard title="Total Boardings"       value={overview.totalBoardingPassengers?.toLocaleString()} icon={FiUsers} color="green" />
          <StatCard title="Busiest Station"       value={overview.busiestStation}        icon={FiTrendingUp} color="red"    subtitle={`${overview.busiestStationPassengers?.toLocaleString()} pax`} />
          <StatCard title="Least Busy Station"    value={overview.leastBusyStation}      icon={FiMapPin}    color="yellow" subtitle={`${overview.leastBusyStationPassengers?.toLocaleString()} pax`} />
          <StatCard title="Routes Analysed"       value={overview.totalRoutesAnalysed}   icon={FiActivity}  color="purple" />
          <StatCard title="Avg / Station"         value={Math.round(overview.avgBoardingPerStation)?.toLocaleString()} icon={FiUsers} color="indigo" subtitle="passengers" />
        </div>
      )}

      {TabBar}

      {/* ── OVERVIEW TAB ── */}
      {tab === TAB_OVERVIEW && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Top-5 busiest */}
            <div className="card">
              <h2 className="text-sm font-semibold text-gray-700 mb-4">Top 5 Busiest Stations</h2>
              {ranked.slice(0,5).length > 0 ? (
                <ResponsiveContainer width="100%" height={200}>
                  <BarChart data={ranked.slice(0,5).map(s => ({ name: s.stationName.split(' ')[0], pax: s.totalBoarding }))}
                    layout="vertical">
                    <XAxis type="number" tick={{fontSize:10}} />
                    <YAxis dataKey="name" type="category" tick={{fontSize:10}} width={90}/>
                    <Tooltip formatter={v => [v.toLocaleString(), 'Passengers']}/>
                    <Bar dataKey="pax" radius={[0,4,4,0]}>
                      {ranked.slice(0,5).map((s,i) => <Cell key={i} fill={DEMAND_COLORS[s.demandLevel] || '#2563eb'}/>)}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              ) : <div className="text-center text-gray-400 py-8">No data</div>}
            </div>

            {/* Bottom-5 least busy */}
            <div className="card">
              <h2 className="text-sm font-semibold text-gray-700 mb-4">Least Busy Stations</h2>
              {ranked.length > 0 ? (
                <ResponsiveContainer width="100%" height={200}>
                  <BarChart
                    data={[...ranked].reverse().slice(0,5).map(s => ({ name: s.stationName.split(' ')[0], pax: s.totalBoarding }))}
                    layout="vertical">
                    <XAxis type="number" tick={{fontSize:10}} />
                    <YAxis dataKey="name" type="category" tick={{fontSize:10}} width={90}/>
                    <Tooltip formatter={v => [v.toLocaleString(), 'Passengers']}/>
                    <Bar dataKey="pax" fill="#94a3b8" radius={[0,4,4,0]}/>
                  </BarChart>
                </ResponsiveContainer>
              ) : <div className="text-center text-gray-400 py-8">No data</div>}
            </div>
          </div>

          {/* Boarding trend mini */}
          <div className="card">
            <h2 className="text-sm font-semibold text-gray-700 mb-4">Daily Boarding Trend (All Stations)</h2>
            {trends.length > 0 ? (
              <ResponsiveContainer width="100%" height={180}>
                <LineChart data={trends.slice(-60)}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0"/>
                  <XAxis dataKey="date" tick={{fontSize:9}} interval={Math.floor(trends.length/8)} />
                  <YAxis tick={{fontSize:10}}/>
                  <Tooltip formatter={v => [v.toLocaleString(), 'Passengers']}/>
                  <Line type="monotone" dataKey="passengers" stroke="#2563eb" dot={false} strokeWidth={2}/>
                </LineChart>
              </ResponsiveContainer>
            ) : <div className="text-center text-gray-400 py-4">No trend data</div>}
          </div>
        </div>
      )}

      {/* ── STATION RANKINGS TAB ── */}
      {tab === TAB_BUSIEST && (
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-gray-700">All Stations — Boarding Rank</h2>
            <span className="text-xs text-gray-400">{ranked.length} stations</span>
          </div>
          <div className="table-container">
            <table className="table-base">
              <thead>
                <tr>
                  <th className="th">#</th>
                  <th className="th">Station</th>
                  <th className="th">City / State</th>
                  <th className="th">Total Boarding</th>
                  <th className="th">Routes</th>
                  <th className="th">Trips</th>
                  <th className="th">Demand</th>
                  <th className="th">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {ranked.map(s => (
                  <tr key={s.stationId} className="hover:bg-gray-50">
                    <td className="td text-gray-400 font-mono text-xs">{s.rank}</td>
                    <td className="td font-medium text-sm">{s.stationName}</td>
                    <td className="td text-xs text-gray-500">{s.city}, {s.state}</td>
                    <td className="td font-bold">{s.totalBoarding.toLocaleString()}</td>
                    <td className="td">{s.routeCount}</td>
                    <td className="td">{s.tripCount}</td>
                    <td className="td">
                      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${DEMAND_BG[s.demandLevel]}`}>
                        {s.demandLevel}
                      </span>
                    </td>
                    <td className="td">
                      <div className="flex gap-2">
                        <button onClick={() => openStationRoutes(s)}
                          className="text-xs text-blue-600 hover:underline flex items-center gap-1">
                          Routes <FiChevronRight size={12}/>
                        </button>
                        <button onClick={() => openPeakHours(s)}
                          className="text-xs text-purple-600 hover:underline flex items-center gap-1">
                          Peak <FiActivity size={12}/>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {ranked.length === 0 && (
                  <tr><td colSpan={8} className="td text-center text-gray-400 py-8">No boarding data for this period</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ── ROUTE BREAKDOWN TAB ── */}
      {tab === TAB_ROUTES && selectedStation && (
        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <button onClick={() => { setSelectedStation(null); setTab(TAB_BUSIEST) }}
              className="text-sm text-blue-600 hover:underline flex items-center gap-1">
              ← Back to Rankings
            </button>
            <h2 className="text-sm font-semibold text-gray-800">
              Routes serving: <span className="text-blue-700">{selectedStation.stationName}</span>
            </h2>
            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${DEMAND_BG[selectedStation.demandLevel]}`}>
              {selectedStation.totalBoarding?.toLocaleString()} total boardings
            </span>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="card">
              <h3 className="text-xs font-semibold text-gray-600 mb-3">Passenger Share by Route</h3>
              {routesLoading ? <div className="flex justify-center py-8"><LoadingSpinner /></div>
                : routes.length > 0 ? (
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={routes.map(r => ({ name: r.routeName.length > 18 ? r.routeName.slice(0,18)+'…' : r.routeName, pax: r.passengers }))}
                    layout="vertical">
                    <XAxis type="number" tick={{fontSize:10}} />
                    <YAxis dataKey="name" type="category" tick={{fontSize:9}} width={130}/>
                    <Tooltip formatter={v => [v.toLocaleString(), 'Passengers']}/>
                    <Bar dataKey="pax" fill="#7c3aed" radius={[0,4,4,0]}/>
                  </BarChart>
                </ResponsiveContainer>
              ) : <div className="text-center text-gray-400 py-8">No route data</div>}
            </div>

            <div className="card">
              <h3 className="text-xs font-semibold text-gray-600 mb-3">Route Contribution %</h3>
              {routes.length > 0 ? (
                <ResponsiveContainer width="100%" height={220}>
                  <PieChart>
                    <Pie data={routes} dataKey="passengers" nameKey="routeName" cx="50%" cy="50%" outerRadius={80} label={({name, pctOfStation}) => `${pctOfStation}%`}>
                      {routes.map((_, i) => <Cell key={i} fill={['#2563eb','#7c3aed','#16a34a','#f59e0b','#ef4444','#06b6d4','#ec4899','#84cc16'][i%8]}/>)}
                    </Pie>
                    <Tooltip formatter={(v, n) => [v.toLocaleString(), n]}/>
                  </PieChart>
                </ResponsiveContainer>
              ) : null}
            </div>
          </div>

          <div className="table-container">
            <table className="table-base">
              <thead>
                <tr>
                  <th className="th">Route</th>
                  <th className="th">Origin</th>
                  <th className="th">Destination</th>
                  <th className="th">Passengers</th>
                  <th className="th">Trips</th>
                  <th className="th">Avg / Trip</th>
                  <th className="th">Share %</th>
                  <th className="th">Trips</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {routesLoading ? (
                  <tr><td colSpan={8} className="td text-center py-6"><LoadingSpinner /></td></tr>
                ) : routes.map(r => (
                  <tr key={r.routeId} className="hover:bg-gray-50">
                    <td className="td font-medium text-xs">{r.routeName}</td>
                    <td className="td text-xs text-gray-500">{r.origin}</td>
                    <td className="td text-xs text-gray-500">{r.destination}</td>
                    <td className="td font-bold">{r.passengers.toLocaleString()}</td>
                    <td className="td">{r.tripCount}</td>
                    <td className="td">{r.avgPerTrip}</td>
                    <td className="td">
                      <div className="flex items-center gap-2">
                        <div className="flex-1 h-1.5 bg-gray-200 rounded-full overflow-hidden">
                          <div className="h-full bg-purple-500 rounded-full" style={{ width: `${Math.min(r.pctOfStation, 100)}%` }}/>
                        </div>
                        <span className="text-xs font-medium w-10 text-right">{r.pctOfStation}%</span>
                      </div>
                    </td>
                    <td className="td">
                      <button onClick={() => openRouteTrips(r)}
                        className="text-xs text-blue-600 hover:underline flex items-center gap-1">
                        Trips <FiChevronRight size={12}/>
                      </button>
                    </td>
                  </tr>
                ))}
                {!routesLoading && routes.length === 0 && (
                  <tr><td colSpan={8} className="td text-center text-gray-400 py-8">No routes found for this station in the selected period</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ── TRIP DETAILS TAB ── */}
      {tab === TAB_TRIPS && selectedStation && selectedRoute && (
        <div className="space-y-4">
          <div className="flex items-center gap-3 flex-wrap">
            <button onClick={() => setTab(TAB_ROUTES)} className="text-sm text-blue-600 hover:underline">
              ← {selectedStation.stationName}
            </button>
            <FiChevronRight size={14} className="text-gray-400"/>
            <span className="text-sm font-semibold text-gray-800">{selectedRoute.routeName}</span>
            <span className="text-xs text-gray-500">({selectedRoute.passengers?.toLocaleString()} total boarding passengers, {selectedRoute.tripCount} trips)</span>
          </div>

          <div className="card">
            <h3 className="text-xs font-semibold text-gray-600 mb-4">
              Trip-wise Boarding — {selectedStation.stationName} on {selectedRoute.routeName}
            </h3>
            <div className="table-container">
              <table className="table-base">
                <thead>
                  <tr>
                    <th className="th">Trip ID</th>
                    <th className="th">Bus</th>
                    <th className="th">Departure</th>
                    <th className="th">Boarding Passengers</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {tripsLoading ? (
                    <tr><td colSpan={4} className="td text-center py-6"><LoadingSpinner /></td></tr>
                  ) : trips.map(t => (
                    <tr key={t.tripId} className="hover:bg-gray-50">
                      <td className="td font-mono text-xs text-gray-500">#{t.tripId}</td>
                      <td className="td font-medium text-xs">{t.busNumber}</td>
                      <td className="td text-xs">{t.departureTime}</td>
                      <td className="td">
                        <div className="flex items-center gap-2">
                          <span className="font-bold">{t.boardingPassengers}</span>
                          <div className="flex-1 h-1.5 bg-gray-100 rounded-full max-w-24">
                            <div className="h-full bg-blue-400 rounded-full"
                              style={{ width: `${Math.min((t.boardingPassengers / (trips[0]?.boardingPassengers || 1)) * 100, 100)}%` }}/>
                          </div>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {!tripsLoading && trips.length === 0 && (
                    <tr><td colSpan={4} className="td text-center text-gray-400 py-8">No trips found</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ── PEAK HOURS TAB ── */}
      {tab === TAB_PEAK && (
        <div className="space-y-4">
          <div className="flex items-center gap-3 flex-wrap mb-2">
            <span className="text-sm text-gray-600">Peak hours for:</span>
            <div className="flex flex-wrap gap-2">
              {ranked.slice(0,8).map(s => (
                <button key={s.stationId} onClick={() => openPeakHours(s)}
                  className={`px-3 py-1 rounded-full text-xs border transition-colors
                    ${peakStation?.stationId === s.stationId
                      ? 'bg-purple-600 text-white border-purple-600'
                      : 'bg-white text-gray-600 border-gray-300 hover:border-purple-400'}`}>
                  {s.stationName.split(' ')[0]}
                </button>
              ))}
            </div>
          </div>

          {peakStation && (
            <div className="card">
              <h3 className="text-sm font-semibold text-gray-700 mb-1">
                {peakStation.stationName} — Peak Boarding Hours
              </h3>
              <p className="text-xs text-gray-400 mb-4">Based on actual or recorded boarding timestamps</p>
              {peakLoading ? (
                <div className="flex justify-center py-10"><LoadingSpinner /></div>
              ) : peakHours.length > 0 ? (
                <>
                  <ResponsiveContainer width="100%" height={260}>
                    <BarChart data={peakHours}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0"/>
                      <XAxis dataKey="label" tick={{fontSize:9}} angle={-30} textAnchor="end" height={50}/>
                      <YAxis tick={{fontSize:10}}/>
                      <Tooltip formatter={v => [v.toLocaleString(), 'Passengers']}/>
                      <Bar dataKey="passengers" radius={[4,4,0,0]}>
                        {peakHours.map((h,i) => (
                          <Cell key={i} fill={h.passengers === Math.max(...peakHours.map(x=>x.passengers)) ? '#dc2626' : '#7c3aed'}/>
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                  <div className="mt-4 grid grid-cols-2 md:grid-cols-4 gap-3">
                    {peakHours.slice().sort((a,b)=>b.passengers-a.passengers).slice(0,4).map((h,i) => (
                      <div key={i} className={`p-3 rounded-lg text-center ${i===0 ? 'bg-red-50 border border-red-200' : 'bg-gray-50'}`}>
                        <p className="text-xs text-gray-500">{i===0 ? '🔥 Peak' : `#${i+1}`}</p>
                        <p className="text-sm font-bold text-gray-800">{h.label}</p>
                        <p className="text-xs text-gray-600">{h.passengers.toLocaleString()} pax</p>
                      </div>
                    ))}
                  </div>
                </>
              ) : <div className="text-center text-gray-400 py-10">No peak hour data available</div>}
            </div>
          )}

          {!peakStation && (
            <div className="card text-center py-10 text-gray-400">
              <FiActivity size={32} className="mx-auto mb-2 opacity-40"/>
              Select a station above to view its peak boarding hours
            </div>
          )}
        </div>
      )}

      {/* ── BOARDING TRENDS TAB ── */}
      {tab === TAB_TRENDS && (
        <div className="card">
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Daily Boarding Trend — All Stations</h2>
          {trends.length > 0 ? (
            <ResponsiveContainer width="100%" height={320}>
              <LineChart data={trends}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0"/>
                <XAxis dataKey="date" tick={{fontSize:9}} interval={Math.floor(trends.length/10)} />
                <YAxis tick={{fontSize:10}}/>
                <Tooltip formatter={v => [v.toLocaleString(), 'Passengers']}/>
                <Legend />
                <Line type="monotone" dataKey="passengers" stroke="#2563eb" dot={false} strokeWidth={2} name="Daily Boardings"/>
              </LineChart>
            </ResponsiveContainer>
          ) : <div className="text-center text-gray-400 py-16">No trend data for this period</div>}
        </div>
      )}

      {/* ── MAP TAB ── */}
      {tab === TAB_MAP && (
        <div className="card p-0 overflow-hidden">
          <div className="p-4 border-b border-gray-100 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold text-gray-700">Station Demand Map</h2>
              <p className="text-xs text-gray-400 mt-0.5">
                Marker size and colour indicate boarding demand · Click a station for details
              </p>
            </div>
            <div className="flex items-center gap-3 text-xs">
              {Object.entries(DEMAND_COLORS).map(([k,c]) => (
                <span key={k} className="flex items-center gap-1">
                  <span className="inline-block w-3 h-3 rounded-full" style={{background:c}}/>
                  {k}
                </span>
              ))}
            </div>
          </div>
          {mapStations.length > 0 ? (
            <MapContainer
              center={[13.0827, 80.2707]}
              zoom={6}
              style={{ height: '500px', width: '100%' }}
              scrollWheelZoom={false}>
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              {mapStations.map(s => {
                const maxPax = mapStations[0]?.boardingPassengers || 1
                const r = 8 + Math.round((s.boardingPassengers / maxPax) * 22)
                return (
                  <CircleMarker
                    key={s.stationId}
                    center={[s.latitude, s.longitude]}
                    radius={r}
                    pathOptions={{
                      fillColor: DEMAND_COLORS[s.demandLevel] || '#2563eb',
                      fillOpacity: 0.8,
                      color: '#fff',
                      weight: 2,
                    }}>
                    <Popup>
                      <div className="text-sm min-w-40">
                        <p className="font-bold text-gray-800">{s.stationName}</p>
                        <p className="text-xs text-gray-500">{s.city}, {s.state}</p>
                        <div className="mt-2 space-y-1 text-xs">
                          <p><span className="text-gray-500">Boardings:</span> <strong>{s.boardingPassengers.toLocaleString()}</strong></p>
                          <p><span className="text-gray-500">Routes:</span> <strong>{s.routeCount}</strong></p>
                          <p><span className="text-gray-500">Demand:</span>
                            <span className={`ml-1 px-1.5 py-0.5 rounded text-white text-xs`} style={{background: DEMAND_COLORS[s.demandLevel]}}>
                              {s.demandLevel}
                            </span>
                          </p>
                        </div>
                        <button
                          onClick={() => openStationRoutes({ stationId: s.stationId, stationName: s.stationName, demandLevel: s.demandLevel, totalBoarding: s.boardingPassengers })}
                          className="mt-2 text-xs text-blue-600 hover:underline">
                          View route breakdown →
                        </button>
                      </div>
                    </Popup>
                  </CircleMarker>
                )
              })}
            </MapContainer>
          ) : (
            <div className="flex items-center justify-center py-20 text-gray-400">
              <div className="text-center">
                <FiMap size={40} className="mx-auto mb-2 opacity-30"/>
                <p>No station coordinate data available</p>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ── RECOMMENDATIONS TAB ── */}
      {tab === TAB_RECS && (
        <div className="space-y-4">
          <div className="flex items-center justify-between mb-2">
            <h2 className="text-sm font-semibold text-gray-700">Smart Analytics Recommendations</h2>
            <span className="text-xs text-gray-400">{recs.length} recommendation{recs.length !== 1 ? 's' : ''}</span>
          </div>
          {recs.length > 0 ? recs.map((r, i) => (
            <div key={i} className={`card border-l-4 ${r.severity === 'HIGH' ? 'border-l-red-500' : r.severity === 'MEDIUM' ? 'border-l-yellow-500' : 'border-l-green-500'}`}>
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-bold
                      ${r.severity === 'HIGH' ? 'bg-red-100 text-red-700' : r.severity === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' : 'bg-green-100 text-green-700'}`}>
                      {r.severity}
                    </span>
                    <span className="text-sm font-semibold text-gray-800">{r.stationName}</span>
                    {r.routeName && <span className="text-xs text-gray-500">· {r.routeName}</span>}
                  </div>
                  <p className="text-xs font-medium text-gray-700 mb-1">📊 {r.pattern}</p>
                  <p className="text-xs text-gray-600 mb-1">✅ <strong>Action:</strong> {r.suggestedAction}</p>
                  <p className="text-xs text-gray-500">ℹ️ {r.reason}</p>
                </div>
                <div className="text-right text-xs text-gray-400 flex-shrink-0">
                  <p className="font-bold text-gray-700 text-sm">{r.passengers?.toLocaleString()}</p>
                  <p>passengers</p>
                </div>
              </div>
            </div>
          )) : (
            <div className="card text-center py-12 text-gray-400">
              <FiAlertCircle size={36} className="mx-auto mb-2 opacity-30"/>
              <p>No recommendations for this period</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
