import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { bookingApi } from '../../api'
import StatusBadge from '../../components/common/StatusBadge'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import EmptyState from '../../components/common/EmptyState'
import { FiTruck } from 'react-icons/fi'
import { formatDateTime } from '../../utils/dateUtils'

export default function MyBookingsPage() {
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [filter, setFilter] = useState('ALL')

  useEffect(() => {
    bookingApi.getMyBookings()
      .then(r => setBookings(r.data.data || []))
      .catch(() => setError('Failed to load your bookings'))
      .finally(() => setLoading(false))
  }, [])

  const filtered = filter === 'ALL' ? bookings : bookings.filter(b => b.status === filter)

  if (loading) return <LoadingSpinner />
  if (error) return <div className="text-red-500 text-center py-12">{error}</div>

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">My Bookings</h1>
      <div className="flex gap-2 mb-4 flex-wrap">
        {['ALL', 'CONFIRMED', 'COMPLETED', 'CANCELLED'].map(s => (
          <button key={s} onClick={() => setFilter(s)}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${filter === s ? 'bg-blue-900 text-white' : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'}`}>
            {s}
          </button>
        ))}
      </div>
      {filtered.length === 0 ? (
        <EmptyState icon={FiTruck} title="No bookings" message="Book your first journey!" />
      ) : (
        <div className="space-y-4">
          {filtered.map(b => (
            <div key={b.id} className="card hover:shadow-md transition-shadow">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                  <div className="flex items-center gap-3 mb-1">
                    <span className="font-mono text-sm font-bold text-blue-700">{b.bookingRef}</span>
                    <StatusBadge status={b.status} />
                  </div>
                  <div className="font-semibold text-gray-800">{b.trip?.routeName}</div>
                  <div className="text-sm text-gray-500">
                    {formatDateTime(b.trip?.departureTime)} &middot;{' '}
                    {b.passengerCount} passenger{b.passengerCount > 1 ? 's' : ''} &middot;{' '}
                    &#8377;{b.totalAmount}
                  </div>
                </div>
                <Link to={`/my-bookings/${b.id}`} className="btn-secondary text-sm py-1.5 px-4 whitespace-nowrap">View Details</Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
