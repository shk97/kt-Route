import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { stationApi, tripApi } from '../../api'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import EmptyState from '../../components/common/EmptyState'
import { FiTruck, FiClock, FiUsers, FiArrowRight, FiSearch, FiMapPin, FiRefreshCw } from 'react-icons/fi'
import toast from 'react-hot-toast'
import { formatTime } from '../../utils/dateUtils'

function localDateString() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export default function SearchPage() {
  const [stations, setStations] = useState([])
  const [form, setForm] = useState({ startStationId: '', endStationId: '', date: localDateString() })
  const [trips, setTrips] = useState([])
  const [searched, setSearched] = useState(false)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    stationApi.getAll()
      .then(r => setStations(r.data.data || []))
      .catch(() => toast.error('Failed to load stations'))
  }, [])

  const handleSearch = async (e) => {
    e.preventDefault()
    if (!form.startStationId || !form.endStationId) { toast.error('Select both origin and destination'); return }
    if (form.startStationId === form.endStationId) { toast.error('Origin and destination must differ'); return }
    setLoading(true)
    try {
      const res = await tripApi.search(form.startStationId, form.endStationId, form.date)
      setTrips(res.data.data || [])
      setSearched(true)
    } catch {
      toast.error('Search failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-10 animate-fade-in">
      {/* Hero search panel */}
      <div className="text-center mb-8">
        <h1 className="text-3xl font-extrabold text-navy-900 tracking-tight">Find Your Bus</h1>
        <p className="text-navy-400 text-sm mt-2">Search buses across South India</p>
      </div>

      <div className="card mb-8 border-navy-200/40"
        style={{ boxShadow: '0 4px 24px rgba(12,25,41,0.1)' }}>
        <form onSubmit={handleSearch}>
          <div className="grid md:grid-cols-4 gap-4 mb-4">
            <div>
              <label className="form-label">
                <FiMapPin size={10} className="inline mr-1" />From
              </label>
              <select value={form.startStationId} onChange={e => setForm({ ...form, startStationId: e.target.value })} className="input-field" required>
                <option value="">Select Origin</option>
                {stations.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
            <div>
              <label className="form-label">
                <FiMapPin size={10} className="inline mr-1" />To
              </label>
              <select value={form.endStationId} onChange={e => setForm({ ...form, endStationId: e.target.value })} className="input-field" required>
                <option value="">Select Destination</option>
                {stations.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
            <div>
              <label className="form-label">
                <FiClock size={10} className="inline mr-1" />Date
              </label>
              <input type="date" value={form.date} min={localDateString()}
                onChange={e => setForm({ ...form, date: e.target.value })} className="input-field" required />
            </div>
            <div className="flex items-end">
              <button type="submit" aria-label="Search buses" className="btn-primary w-full py-2.5">
                <FiSearch size={16} />Search
              </button>
            </div>
          </div>
        </form>
      </div>

      {loading && <LoadingSpinner message="Searching available buses..." />}

      {!loading && searched && trips.length === 0 && (
        <div className="card text-center py-16">
          <FiTruck size={40} className="text-navy-200 mx-auto mb-4" />
          <h3 className="font-bold text-navy-800 text-lg mb-2">No buses found</h3>
          <p className="text-navy-400 text-sm mb-6">Try a different date or route</p>
          <button onClick={() => setSearched(false)} className="btn-secondary text-sm gap-2">
            <FiRefreshCw size={14} />Try Again
          </button>
        </div>
      )}

      {!loading && trips.length > 0 && (
        <div>
          <div className="flex items-center justify-between mb-4">
            <p className="text-sm font-semibold text-navy-600">
              {trips.length} {trips.length === 1 ? 'bus' : 'buses'} found
            </p>
          </div>
          <div className="space-y-4">
            {trips.map(trip => (
              <div key={trip.id} className="card-hover">
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-5">
                  {/* Route info */}
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-3">
                      <div className="w-9 h-9 rounded-xl bg-navy-50 flex items-center justify-center flex-shrink-0">
                        <FiTruck size={17} className="text-navy-600" />
                      </div>
                      <div>
                        <div className="font-bold text-navy-900">{trip.routeName}</div>
                        <div className="text-xs text-navy-400">{trip.bus?.busNumber}</div>
                      </div>
                      <span className="badge-blue text-xs ml-1">{trip.bus?.busType}</span>
                    </div>

                    {/* Time + stops */}
                    <div className="flex items-center gap-4 mb-3">
                      <div className="text-center">
                        <div className="text-lg font-extrabold text-navy-900">{formatTime(trip.departureTime)}</div>
                        <div className="text-[10px] text-navy-400 font-medium">{trip.startStation?.name}</div>
                      </div>
                      <div className="flex-1 flex items-center gap-2">
                        <div className="flex-1 border-t border-dashed border-navy-200" />
                        <FiArrowRight size={13} className="text-navy-400 flex-shrink-0" />
                        <div className="flex-1 border-t border-dashed border-navy-200" />
                      </div>
                      <div className="text-center">
                        <div className="text-lg font-extrabold text-navy-900">{formatTime(trip.arrivalTime)}</div>
                        <div className="text-[10px] text-navy-400 font-medium">{trip.endStation?.name}</div>
                      </div>
                    </div>

                    <div className="flex items-center gap-4 text-xs text-navy-500">
                      <span className="flex items-center gap-1">
                        <FiUsers size={12} />
                        <span className={trip.availableSeats <= 5 ? 'text-red-500 font-semibold' : ''}>
                          {trip.availableSeats} seats left
                        </span>
                      </span>
                    </div>
                  </div>

                  {/* Price + Book */}
                  <div className="flex md:flex-col items-center md:items-end gap-4 md:gap-2">
                    <div className="text-right">
                      <div className="text-2xl font-extrabold text-navy-900">&#8377;{trip.basePrice}</div>
                      <div className="text-[10px] text-navy-400">per seat</div>
                    </div>
                    <button
                      onClick={() => navigate(`/book/${trip.id}`)}
                      disabled={trip.availableSeats <= 0}
                      className="btn-primary px-6 py-2.5 whitespace-nowrap text-sm">
                      {trip.availableSeats > 0 ? 'Book Now' : 'Sold Out'}
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
