import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { bookingApi } from '../../api'
import StatusBadge from '../../components/common/StatusBadge'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import { FiTruck, FiCalendar, FiSearch } from 'react-icons/fi'

export default function PassengerDashboard() {
  const { user } = useAuth()
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    bookingApi.getMyBookings().then(r => setBookings(r.data.data || [])).finally(() => setLoading(false))
  }, [])

  const recent = bookings.slice(0, 5)

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Welcome, {user?.fullName}!</h1>
        <p className="text-gray-500">Your travel dashboard</p>
      </div>

      <div className="grid md:grid-cols-3 gap-4 mb-8">
        <div className="card text-center hover:shadow-md transition-shadow cursor-pointer">
          <Link to="/search">
            <FiSearch size={32} className="text-blue-700 mx-auto mb-2" />
            <div className="font-semibold text-gray-700">Search Buses</div>
            <div className="text-xs text-gray-400">Find and book your journey</div>
          </Link>
        </div>
        <div className="card text-center">
          <FiTruck size={32} className="text-green-600 mx-auto mb-2" />
          <div className="text-2xl font-bold text-gray-800">{bookings.filter(b => b.status === 'CONFIRMED').length}</div>
          <div className="text-xs text-gray-400">Active Bookings</div>
        </div>
        <div className="card text-center">
          <FiCalendar size={32} className="text-blue-600 mx-auto mb-2" />
          <div className="text-2xl font-bold text-gray-800">{bookings.length}</div>
          <div className="text-xs text-gray-400">Total Bookings</div>
        </div>
      </div>

      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-800">Recent Bookings</h2>
          <Link to="/my-bookings" className="text-sm text-blue-700 hover:underline">View All</Link>
        </div>
        {loading ? <LoadingSpinner /> : (
          <div className="table-container">
            <table className="table-base">
              <thead><tr>
                <th className="th">Ref</th><th className="th">Route</th>
                <th className="th">Date</th><th className="th">Status</th>
              </tr></thead>
              <tbody className="divide-y divide-gray-100">
                {recent.map(b => (
                  <tr key={b.id} className="hover:bg-gray-50">
                    <td className="td font-mono text-xs">{b.bookingRef}</td>
                    <td className="td">{b.trip?.routeName}</td>
                    <td className="td text-xs">{b.trip?.departureTime ? new Date(b.trip.departureTime).toLocaleDateString() : '-'}</td>
                    <td className="td"><StatusBadge status={b.status} /></td>
                  </tr>
                ))}
                {recent.length === 0 && <tr><td colSpan={4} className="td text-center text-gray-400">No bookings yet</td></tr>}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
