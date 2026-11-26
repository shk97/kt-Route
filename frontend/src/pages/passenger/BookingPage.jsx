import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { tripApi, bookingApi } from '../../api'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import toast from 'react-hot-toast'
import { FiPlus, FiMinus, FiTruck, FiMapPin, FiCalendar, FiCreditCard } from 'react-icons/fi'
import { formatDateTime } from '../../utils/dateUtils'

const newPassenger = () => ({ id: crypto.randomUUID(), passengerName: '', age: '', seatNumber: '' })

export default function BookingPage() {
  const { tripId } = useParams()
  const navigate = useNavigate()
  const [trip, setTrip] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [form, setForm] = useState({
    boardingStationId: '',
    alightingStationId: '',
    passengers: [newPassenger()]
  })

  useEffect(() => {
    tripApi.getById(tripId)
      .then(r => setTrip(r.data.data))
      .catch(() => setError('Failed to load trip details'))
      .finally(() => setLoading(false))
  }, [tripId])

  const addPassenger = () => {
    if (form.passengers.length >= 10) return
    setForm({ ...form, passengers: [...form.passengers, newPassenger()] })
  }

  const removePassenger = (id) => {
    if (form.passengers.length === 1) return
    setForm({ ...form, passengers: form.passengers.filter(p => p.id !== id) })
  }

  const updatePassenger = (id, key, val) => {
    setForm({ ...form, passengers: form.passengers.map(p => p.id === id ? { ...p, [key]: val } : p) })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.boardingStationId || !form.alightingStationId) { toast.error('Select boarding and alighting stations'); return }
    if (form.boardingStationId === form.alightingStationId) { toast.error('Boarding and alighting stations must differ'); return }
    setSubmitting(true)
    try {
      const payload = {
        tripId: Number(tripId),
        boardingStationId: Number(form.boardingStationId),
        alightingStationId: Number(form.alightingStationId),
        passengers: form.passengers.map(({ id: _id, ...p }) => ({ ...p, age: Number(p.age) }))
      }
      const res = await bookingApi.create(payload)
      toast.success('Booking confirmed!')
      navigate('/my-bookings/' + res.data.data.id)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Booking failed')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <LoadingSpinner />
  if (error) return <div className="text-red-500 text-center py-16">{error}</div>
  if (!trip) return <div className="text-center py-16 text-navy-400">Trip not found</div>

  const sortedStops = [...(trip.route?.stops ?? [])].sort((a, b) => a.stopOrder - b.stopOrder)
  const total = form.passengers.length * trip.basePrice

  return (
    <div className="max-w-2xl mx-auto px-4 py-10 animate-fade-in">
      <h1 className="text-2xl font-extrabold text-navy-900 tracking-tight mb-2">Book Your Seat</h1>
      <p className="text-navy-400 text-sm mb-7">Complete your journey details below</p>

      {/* Trip summary card */}
      <div className="rounded-2xl mb-6 overflow-hidden" style={{ background: 'linear-gradient(135deg, #06101f, #132238)' }}>
        <div className="px-6 py-5">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-9 h-9 rounded-xl bg-gold-gradient flex items-center justify-center flex-shrink-0 shadow-gold">
              <FiTruck size={16} className="text-white" />
            </div>
            <div>
              <div className="text-white font-bold">{trip.routeName}</div>
              <div className="text-navy-300 text-xs">{trip.bus?.busNumber} &middot; {trip.bus?.busType}</div>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div className="flex items-start gap-2 text-navy-300">
              <FiCalendar size={14} className="mt-0.5 flex-shrink-0 text-gold-400" />
              <span>{formatDateTime(trip.departureTime)}</span>
            </div>
            <div className="flex items-start gap-2 text-navy-300">
              <FiMapPin size={14} className="mt-0.5 flex-shrink-0 text-gold-400" />
              <span>{trip.availableSeats} seats available</span>
            </div>
          </div>
        </div>
        <div className="bg-white/8 border-t border-white/10 px-6 py-3 flex justify-between items-center">
          <span className="text-navy-300 text-sm">Price per seat</span>
          <span className="text-white font-extrabold text-lg">&#8377;{trip.basePrice}</span>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        {/* Journey details */}
        <div className="card">
          <h3 className="font-bold text-navy-800 mb-4">Journey Details</h3>
          <div className="grid md:grid-cols-2 gap-4">
            <div>
              <label className="form-label">Boarding Station</label>
              <select value={form.boardingStationId} onChange={e => setForm({ ...form, boardingStationId: e.target.value })} className="input-field" required>
                <option value="">Select station</option>
                {sortedStops.map(s => <option key={s.station?.id} value={s.station?.id}>{s.station?.name}</option>)}
              </select>
            </div>
            <div>
              <label className="form-label">Alighting Station</label>
              <select value={form.alightingStationId} onChange={e => setForm({ ...form, alightingStationId: e.target.value })} className="input-field" required>
                <option value="">Select station</option>
                {sortedStops.map(s => <option key={s.station?.id} value={s.station?.id}>{s.station?.name}</option>)}
              </select>
            </div>
          </div>
        </div>

        {/* Passengers */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-bold text-navy-800">Passengers <span className="text-navy-400 font-normal text-sm">({form.passengers.length})</span></h3>
            {form.passengers.length < 10 && (
              <button type="button" onClick={addPassenger} aria-label="Add passenger"
                className="btn-secondary text-xs py-1.5 px-3 gap-1">
                <FiPlus size={13} />Add Passenger
              </button>
            )}
          </div>
          <div className="space-y-3">
            {form.passengers.map((p, idx) => (
              <div key={p.id} className="bg-navy-50/60 border border-navy-100 rounded-xl p-4">
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-bold text-navy-600 uppercase tracking-wider">Passenger {idx + 1}</span>
                  {form.passengers.length > 1 &&
                    <button type="button" onClick={() => removePassenger(p.id)} aria-label="Remove passenger"
                      className="text-red-400 hover:text-red-600 transition-colors p-1 rounded">
                      <FiMinus size={14} />
                    </button>
                  }
                </div>
                <div className="grid md:grid-cols-3 gap-3">
                  <input type="text" required placeholder="Full Name" value={p.passengerName}
                    onChange={e => updatePassenger(p.id, 'passengerName', e.target.value)} className="input-field" />
                  <input type="number" required min="1" max="120" placeholder="Age" value={p.age}
                    onChange={e => updatePassenger(p.id, 'age', e.target.value)} className="input-field" />
                  <input type="text" required placeholder="Seat No (e.g. 01)" value={p.seatNumber}
                    onChange={e => updatePassenger(p.id, 'seatNumber', e.target.value)} className="input-field" />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Total + CTA */}
        <div className="card border-gold-200/60"
          style={{ background: 'linear-gradient(135deg, #fffbeb, #fef3c7)' }}>
          <div className="flex justify-between items-center">
            <div>
              <div className="text-xs font-semibold text-gold-700 uppercase tracking-wider mb-1">Total Amount</div>
              <div className="text-navy-500 text-sm">{form.passengers.length} passenger{form.passengers.length > 1 ? 's' : ''}</div>
            </div>
            <div className="text-3xl font-extrabold text-navy-900">&#8377;{total.toFixed(0)}</div>
          </div>
        </div>

        <button type="submit" disabled={submitting} className="btn-primary w-full py-3.5 text-base gap-2">
          <FiCreditCard size={18} />
          {submitting ? 'Confirming Booking...' : 'Confirm Booking'}
        </button>
      </form>
    </div>
  )
}
