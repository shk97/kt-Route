import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { routeApi } from '../../api'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import StatusBadge from '../../components/common/StatusBadge'

export default function RouteDetailPage() {
  const { id } = useParams()
  const [route, setRoute] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    routeApi.getById(id).then(r => setRoute(r.data.data)).finally(() => setLoading(false))
  }, [id])

  if (loading) return <LoadingSpinner />
  if (!route) return <div className="text-center py-16 text-gray-500">Route not found</div>

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h1 className="text-2xl font-bold text-blue-900">{route.name}</h1>
          <StatusBadge status={route.status} />
        </div>
        <div className="grid md:grid-cols-2 gap-4 mb-6 text-sm">
          <div><span className="text-gray-500">Code:</span> <strong>{route.routeCode}</strong></div>
          <div><span className="text-gray-500">Distance:</span> <strong>{route.distanceKm} km</strong></div>
          <div><span className="text-gray-500">Duration:</span> <strong>{Math.floor(route.estimatedDurationMinutes/60)}h {route.estimatedDurationMinutes%60}m</strong></div>
        </div>
        <h3 className="font-semibold text-gray-700 mb-3">Route Stops</h3>
        <div className="space-y-2">
          {route.stops?.sort((a,b) => a.stopOrder - b.stopOrder).map((stop, idx) => (
            <div key={stop.id} className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
              <div className="w-6 h-6 bg-blue-900 text-white rounded-full flex items-center justify-center text-xs font-bold">{idx+1}</div>
              <div>
                <div className="font-medium text-sm">{stop.station?.name}</div>
                <div className="text-xs text-gray-400">{stop.station?.city} &middot; +{stop.arrivalOffsetMinutes} min</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
