import { Link } from 'react-router-dom'
import { FiCheckCircle } from 'react-icons/fi'

export default function BookingConfirmPage() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-center px-4">
      <FiCheckCircle size={64} className="text-green-500 mb-4" />
      <h1 className="text-2xl font-bold text-gray-800 mb-2">Booking Confirmed!</h1>
      <p className="text-gray-500 mb-6">Your ticket has been booked. Check your bookings for details.</p>
      <div className="flex gap-3">
        <Link to="/my-bookings" className="btn-primary">View My Bookings</Link>
        <Link to="/search" className="btn-secondary">Book Another</Link>
      </div>
    </div>
  )
}
