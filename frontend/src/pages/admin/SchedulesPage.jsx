import { useEffect, useState } from 'react'
import { tripApi, routeApi, busApi, driverApi } from '../../api'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import StatusBadge from '../../components/common/StatusBadge'
import Modal from '../../components/common/Modal'
import ConfirmDialog from '../../components/common/ConfirmDialog'
import toast from 'react-hot-toast'
import { FiPlus, FiEdit2, FiXCircle } from 'react-icons/fi'
import { formatDateTime } from '../../utils/dateUtils'

const toInput = (dt) => dt ? new Date(new Date(dt).getTime() - new Date(dt).getTimezoneOffset() * 60000).toISOString().slice(0, 16) : ''
const EMPTY = { routeId: '', busId: '', driverId: '', departureTime: '', arrivalTime: '', basePrice: '', notes: '', changeReason: '' }

export default function SchedulesPage() {
  const [trips, setTrips] = useState([])
  const [routes, setRoutes] = useState([])
  const [buses, setBuses] = useState([])
  const [drivers, setDrivers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [modal, setModal] = useState(false)
  const [editId, setEditId] = useState(null)
  const [form, setForm] = useState(EMPTY)
  const [cancelId, setCancelId] = useState(null)
  const [cancelReason, setCancelReason] = useState('')

  const load = () => tripApi.getAll().then(r => setTrips(r.data.data || []))

  useEffect(() => {
    Promise.all([
      load(),
      routeApi.getAllAdmin().then(r => setRoutes(r.data.data || [])),
      busApi.getAll().then(r => setBuses(r.data.data || [])),
      driverApi.getAll().then(r => setDrivers(r.data.data || []))
    ])
      .catch(() => setError('Failed to load schedules data'))
      .finally(() => setLoading(false))
  }, [])

  const openCreate = () => { setForm(EMPTY); setEditId(null); setModal(true) }
  const openEdit = (t) => {
    setForm({ routeId: t.routeId || '', busId: t.bus?.id || '', driverId: t.driver?.id || '', departureTime: toInput(t.departureTime), arrivalTime: toInput(t.arrivalTime), basePrice: t.basePrice, notes: t.notes || '', changeReason: '' })
    setEditId(t.id); setModal(true)
  }

  const handleSave = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    try {
      const payload = { ...form, routeId: Number(form.routeId), busId: Number(form.busId), driverId: form.driverId ? Number(form.driverId) : null, basePrice: Number(form.basePrice) }
      if (editId) await tripApi.update(editId, payload)
      else await tripApi.create(payload)
      toast.success(`Trip ${editId ? 'updated' : 'created'}`)
      setModal(false)
      load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Save failed')
    } finally {
      setSubmitting(false)
    }
  }

  const handleCancel = async () => {
    try {
      await tripApi.cancel(cancelId, cancelReason || 'Cancelled by admin')
      toast.success('Trip cancelled')
      load()
    } catch {
      toast.error('Cancel failed')
    } finally {
      setCancelId(null)
      setCancelReason('')
    }
  }

  const upd = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  if (loading) return <LoadingSpinner />
  if (error) return <div className="text-red-500 text-center py-12">{error}</div>

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-bold text-gray-800">Schedule Management</h1>
        <button onClick={openCreate} className="btn-primary flex items-center gap-2 text-sm"><FiPlus size={14} />Add Trip</button>
      </div>
      <div className="table-container">
        <table className="table-base">
          <thead><tr>
            <th className="th">Route</th><th className="th">Bus</th><th className="th">Departure</th>
            <th className="th">Price</th><th className="th">Passengers</th><th className="th">Status</th><th className="th">Actions</th>
          </tr></thead>
          <tbody className="divide-y divide-gray-100">
            {trips.map(t => (
              <tr key={t.id} className="hover:bg-gray-50">
                <td className="td text-xs font-medium">{t.routeName}</td>
                <td className="td text-xs">{t.bus?.busNumber}</td>
                <td className="td text-xs">{formatDateTime(t.departureTime)}</td>
                <td className="td text-xs">&#8377;{t.basePrice}</td>
                <td className="td text-xs">{t.confirmedPassengers}/{t.totalSeats}</td>
                <td className="td"><StatusBadge status={t.status} /></td>
                <td className="td">
                  <div className="flex gap-2">
                    {t.status === 'SCHEDULED' && <button onClick={() => openEdit(t)} aria-label="Edit trip" className="text-blue-600 hover:text-blue-800 p-1"><FiEdit2 size={14} /></button>}
                    {t.status === 'SCHEDULED' && <button onClick={() => setCancelId(t.id)} aria-label="Cancel trip" className="text-red-500 hover:text-red-700 p-1"><FiXCircle size={14} /></button>}
                  </div>
                </td>
              </tr>
            ))}
            {trips.length === 0 && <tr><td colSpan={7} className="td text-center text-gray-400">No trips yet</td></tr>}
          </tbody>
        </table>
      </div>

      <Modal isOpen={modal} title={editId ? 'Edit Trip' : 'New Trip'} onClose={() => setModal(false)} size="lg">
        <form onSubmit={handleSave} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Route</label>
              <select required className="input-field text-sm" value={form.routeId} onChange={upd('routeId')}>
                <option value="">Select</option>{routes.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
              </select></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Bus</label>
              <select required className="input-field text-sm" value={form.busId} onChange={upd('busId')}>
                <option value="">Select</option>{buses.filter(b => b.status === 'ACTIVE').map(b => <option key={b.id} value={b.id}>{b.busNumber} ({b.busType})</option>)}
              </select></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Driver (Optional)</label>
              <select className="input-field text-sm" value={form.driverId} onChange={upd('driverId')}>
                <option value="">None</option>{drivers.filter(d => d.isActive).map(d => <option key={d.id} value={d.id}>{d.fullName}</option>)}
              </select></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Price (&#8377;)</label>
              <input required type="number" min="0" className="input-field text-sm" value={form.basePrice} onChange={upd('basePrice')} /></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Departure</label>
              <input required type="datetime-local" className="input-field text-sm" value={form.departureTime} onChange={upd('departureTime')} /></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Arrival</label>
              <input required type="datetime-local" className="input-field text-sm" value={form.arrivalTime} onChange={upd('arrivalTime')} /></div>
          </div>
          {editId && (
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Reason for Change</label>
              <input className="input-field text-sm" value={form.changeReason} onChange={upd('changeReason')} /></div>
          )}
          <div className="flex gap-3 justify-end pt-2">
            <button type="button" onClick={() => setModal(false)} className="btn-secondary text-sm py-1.5 px-4">Cancel</button>
            <button type="submit" disabled={submitting} className="btn-primary text-sm py-1.5 px-4">{editId ? 'Update' : 'Create'}</button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog isOpen={!!cancelId} title="Cancel Trip?" danger
        message="Please provide a reason for cancelling this trip."
        onConfirm={handleCancel}
        onCancel={() => { setCancelId(null); setCancelReason('') }}
        confirmText="Yes, Cancel">
        <label className="block text-xs font-medium text-gray-600 mb-1">Reason</label>
        <input
          className="input-field text-sm"
          value={cancelReason}
          onChange={e => setCancelReason(e.target.value)}
          placeholder="e.g., Bus maintenance"
        />
      </ConfirmDialog>
    </div>
  )
}
