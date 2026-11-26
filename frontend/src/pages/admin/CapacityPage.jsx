import { useEffect, useState } from 'react'
import { analyticsApi } from '../../api'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import StatusBadge from '../../components/common/StatusBadge'

export default function CapacityPage() {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('ALL')

  useEffect(() => {
    analyticsApi.capacity().then(r => setData(r.data.data || [])).finally(() => setLoading(false))
  }, [])

  const filtered = filter === 'ALL' ? data : data.filter(d => d.utilizationCategory === filter)
  if (loading) return <LoadingSpinner />

  return (
    <div>
      <h1 className="text-xl font-bold text-gray-800 mb-6">Capacity &amp; Occupancy</h1>
      <div className="flex gap-3 mb-6 flex-wrap">
        {['ALL','OVER_CAPACITY','NORMAL','UNDERUTILIZED'].map(f => (
          <button key={f} onClick={() => setFilter(f)}
            className={`px-4 py-2 rounded-xl text-sm font-medium border transition-colors ${filter===f ? 'bg-blue-900 text-white border-blue-900' : 'bg-white border-gray-200 text-gray-600 hover:bg-gray-50'}`}>
            {f.replace(/_/g,' ')} {f !== 'ALL' && `(${data.filter(d=>d.utilizationCategory===f).length})`}
          </button>
        ))}
      </div>
      <div className="table-container">
        <table className="table-base">
          <thead><tr>
            <th className="th">Route</th><th className="th">Bus</th><th className="th">Departure</th>
            <th className="th">Seats</th><th className="th">Passengers</th><th className="th">Occupancy</th>
            <th className="th">Category</th><th className="th">Recommendation</th>
          </tr></thead>
          <tbody className="divide-y divide-gray-100">
            {filtered.map((c,i) => (
              <tr key={i} className="hover:bg-gray-50">
                <td className="td text-xs font-medium">{c.routeName}</td>
                <td className="td text-xs">{c.busNumber}</td>
                <td className="td text-xs">{c.departureTime ? new Date(c.departureTime).toLocaleString() : '-'}</td>
                <td className="td">{c.totalSeats}</td>
                <td className="td">{c.confirmedPassengers}</td>
                <td className="td font-semibold">{c.occupancyPercentage}%</td>
                <td className="td"><StatusBadge status={c.utilizationCategory} /></td>
                <td className="td text-xs text-gray-500 max-w-xs truncate">{c.recommendation}</td>
              </tr>
            ))}
            {filtered.length === 0 && <tr><td colSpan={8} className="td text-center text-gray-400">No data</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  )
}
