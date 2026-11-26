import { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { bookingApi, paymentApi } from '../../api'
import StatusBadge from '../../components/common/StatusBadge'
import ConfirmDialog from '../../components/common/ConfirmDialog'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import toast from 'react-hot-toast'
import { FiArrowLeft, FiCreditCard } from 'react-icons/fi'

const PAYMENT_METHODS = [
  { value: 'UPI',         label: 'UPI' },
  { value: 'CREDIT_CARD', label: 'Credit Card' },
  { value: 'DEBIT_CARD',  label: 'Debit Card' },
  { value: 'NET_BANKING', label: 'Net Banking' },
  { value: 'WALLET',      label: 'Wallet' },
]

function loadRazorpayScript() {
  return new Promise(resolve => {
    if (document.getElementById('razorpay-checkout-js')) { resolve(true); return }
    const script = document.createElement('script')
    script.id  = 'razorpay-checkout-js'
    script.src = 'https://checkout.razorpay.com/v1/checkout.js'
    script.onload  = () => resolve(true)
    script.onerror = () => resolve(false)
    document.body.appendChild(script)
  })
}

export default function BookingDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [booking, setBooking]       = useState(null)
  const [loading, setLoading]       = useState(true)
  const [cancelOpen, setCancelOpen] = useState(false)
  const [payMethod, setPayMethod]   = useState('UPI')
  const [paying, setPaying]         = useState(false)

  useEffect(() => {
    bookingApi.getById(id).then(r => setBooking(r.data.data)).finally(() => setLoading(false))
  }, [id])

  const handleCancel = async () => {
    try {
      await bookingApi.cancel(id, 'Passenger requested cancellation')
      toast.success('Booking cancelled')
      setCancelOpen(false)
      setBooking(prev => ({ ...prev, status: 'CANCELLED' }))
    } catch (err) {
      toast.error(err.response?.data?.message || 'Cancel failed')
      setCancelOpen(false)
    }
  }

  const handlePay = useCallback(async () => {
    setPaying(true)
    try {
      const loaded = await loadRazorpayScript()
      if (!loaded) { toast.error('Failed to load payment gateway. Check your internet.'); setPaying(false); return }

      // Step 1 — create Razorpay order on the backend
      const initiateRes = await paymentApi.initiate({ bookingId: booking.id, method: payMethod })
      const { paymentRef, razorpayOrderId, razorpayKeyId, amount, currency } = initiateRes.data.data

      // Step 2 — open Razorpay checkout popup
      const options = {
        key:         razorpayKeyId,
        amount:      Math.round(Number(amount) * 100), // paise
        currency:    currency || 'INR',
        name:        'RouteSync',
        description: `Booking ${booking.bookingRef}`,
        order_id:    razorpayOrderId,
        prefill: {
          name:  booking.user?.fullName  || '',
          email: booking.user?.email     || '',
        },
        theme: { color: '#f59e0b' },

        handler: async function (response) {
          // Step 3 — verify signature on backend and confirm booking
          try {
            await paymentApi.confirm(paymentRef, {
              razorpayPaymentId: response.razorpay_payment_id,
              razorpayOrderId:   response.razorpay_order_id,
              razorpaySignature: response.razorpay_signature,
            })
            toast.success('Payment successful! Ticket sent to your email.')
            setBooking(prev => ({ ...prev, status: 'CONFIRMED' }))
          } catch (err) {
            toast.error(err.response?.data?.message || 'Payment verification failed.')
          } finally {
            setPaying(false)
          }
        },

        modal: {
          ondismiss: async function () {
            // User closed the popup — mark as failed so they can retry
            try { await paymentApi.fail(paymentRef, 'Dismissed by user') } catch (_) {}
            toast('Payment cancelled.', { icon: 'ℹ️' })
            setPaying(false)
          }
        }
      }

      const rzp = new window.Razorpay(options)
      rzp.on('payment.failed', async function (response) {
        try { await paymentApi.fail(paymentRef, response.error?.description || 'Gateway failure') } catch (_) {}
        toast.error('Payment failed: ' + (response.error?.description || 'Unknown error'))
        setPaying(false)
      })
      rzp.open()

    } catch (err) {
      toast.error(err.response?.data?.message || 'Could not start payment. Try again.')
      setPaying(false)
    }
  }, [booking, payMethod])

  if (loading) return <LoadingSpinner />
  if (!booking) return <div className="text-center py-16">Booking not found</div>

  return (
    <div className="max-w-2xl mx-auto">
      <button onClick={() => navigate(-1)} className="flex items-center gap-2 text-gray-500 hover:text-gray-700 mb-4 text-sm">
        <FiArrowLeft size={14} />Back
      </button>

      <div className="card mb-4">
        <div className="flex items-center justify-between mb-4">
          <h1 className="text-xl font-bold text-blue-700 font-mono">{booking.bookingRef}</h1>
          <StatusBadge status={booking.status} />
        </div>
        <div className="grid md:grid-cols-2 gap-3 text-sm mb-4">
          <div><span className="text-gray-500">Route:</span> <strong>{booking.trip?.routeName}</strong></div>
          <div><span className="text-gray-500">Departure:</span> <strong>{booking.trip?.departureTime ? new Date(booking.trip.departureTime).toLocaleString() : '-'}</strong></div>
          <div><span className="text-gray-500">From:</span> <strong>{booking.boardingStation?.name}</strong></div>
          <div><span className="text-gray-500">To:</span> <strong>{booking.alightingStation?.name}</strong></div>
          <div><span className="text-gray-500">Total:</span> <strong className="text-green-700">&#8377;{booking.totalAmount}</strong></div>
          <div><span className="text-gray-500">Bus:</span> <strong>{booking.trip?.bus?.busNumber}</strong></div>
        </div>
        <h3 className="font-semibold text-gray-700 mb-2">Passengers</h3>
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead><tr className="bg-gray-50">
              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Name</th>
              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Age</th>
              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Seat</th>
              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">Fare</th>
            </tr></thead>
            <tbody className="divide-y divide-gray-100">
              {booking.passengers?.map(p => (
                <tr key={p.id}>
                  <td className="px-3 py-2">{p.passengerName}</td>
                  <td className="px-3 py-2">{p.age}</td>
                  <td className="px-3 py-2 font-mono">{p.seatNumber}</td>
                  <td className="px-3 py-2">&#8377;{p.ticketPrice}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Payment method selector — only for PENDING bookings */}
      {booking.status === 'PENDING' && (
        <div className="card mb-4">
          <h3 className="font-semibold text-gray-700 mb-3 text-sm">Select Payment Method</h3>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 mb-4">
            {PAYMENT_METHODS.map(m => (
              <label key={m.value}
                className="flex items-center gap-2 p-2.5 border rounded-lg cursor-pointer text-sm transition-colors"
                style={{ borderColor: payMethod === m.value ? '#f59e0b' : '#e5e7eb', background: payMethod === m.value ? '#fffbeb' : '' }}>
                <input type="radio" name="payMethod" value={m.value}
                  checked={payMethod === m.value} onChange={e => setPayMethod(e.target.value)}
                  className="accent-amber-500" />
                {m.label}
              </label>
            ))}
          </div>
          <button onClick={handlePay} disabled={paying}
            className="btn-primary w-full py-2.5 flex items-center justify-center gap-2 disabled:opacity-60">
            {paying
              ? <><span className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full inline-block" /> Opening payment…</>
              : <><FiCreditCard size={15} /> Pay &#8377;{booking.totalAmount} with Razorpay</>
            }
          </button>
        </div>
      )}

      {booking.status === 'CONFIRMED' && (
        <button onClick={() => setCancelOpen(true)} className="btn-danger text-sm py-2 px-4">Cancel Booking</button>
      )}

      <ConfirmDialog isOpen={cancelOpen} title="Cancel Booking?" danger
        message="This will cancel your booking. Cancellation is not available within 2 hours of departure."
        onConfirm={handleCancel} onCancel={() => setCancelOpen(false)} confirmText="Yes, Cancel" />
    </div>
  )
}
