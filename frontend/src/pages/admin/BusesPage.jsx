import { useEffect, useState } from 'react'
import { busApi } from '../../api'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import StatusBadge from '../../components/common/StatusBadge'
import Modal from '../../components/common/Modal'
import toast from 'react-hot-toast'
import { FiPlus, FiEdit2 } from 'react-icons/fi'

const EMPTY = { busNumber: '', registrationNo: '', busType: 'AC Seater', totalSeats: '', status: 'ACTIVE', manufacturer: '', model: '', manufacturingYear: '' }

export default function BusesPage() {
  const [buses, setBuses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [modal, setModal] = useState(false)
  const [editId, setEditId] = useState(null)
  const [form, setForm] = useState(EMPTY)

  const load = () => busApi.getAll().then(r => setBuses(r.data.data || []))
  useEffect(() => {
    load()
      .catch(() => setError('Failed to load buses'))
      .finally(() => setLoading(false))
  }, [])

  const openCreate = () => { setForm(EMPTY); setEditId(null); setModal(true) }
  const openEdit = (b) => {
    setForm({ busNumber: b.busNumber, registrationNo: b.registrationNo, busType: b.busType, totalSeats: b.totalSeats, status: b.status, manufacturer: b.manufacturer || '', model: b.model || '', manufacturingYear: b.manufacturingYear || '' })
    setEditId(b.id); setModal(true)
  }

  const handleSave = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    try {
      const payload = { ...form, totalSeats: Number(form.totalSeats), manufacturingYear: form.manufacturingYear ? Number(form.manufacturingYear) : null }
      if (editId) await busApi.update(editId, payload)
      else await busApi.create(payload)
      toast.success(`Bus ${editId ? 'updated' : 'created'}`)
      setModal(false); load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Save failed')
    } finally {
      setSubmitting(false)
    }
  }

  const upd = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  if (loading) return <LoadingSpinner />
  if (error) return <div className="text-red-500 text-center py-12">{error}</div>

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-bold text-gray-800">Bus Management</h1>
        <button onClick={openCreate} className="btn-primary flex items-center gap-2 text-sm"><FiPlus size={14} />Add Bus</button>
      </div>
      <div className="table-container">
        <table className="table-base">
          <thead><tr>
            <th className="th">Bus #</th><th className="th">Registration</th><th className="th">Type</th>
            <th className="th">Seats</th><th className="th">Status</th><th className="th">Actions</th>
          </tr></thead>
          <tbody className="divide-y divide-gray-100">
            {buses.map(b => (
              <tr key={b.id} className="hover:bg-gray-50">
                <td className="td font-mono text-xs font-bold">{b.busNumber}</td>
                <td className="td text-xs">{b.registrationNo}</td>
                <td className="td text-xs">{b.busType}</td>
                <td className="td">{b.totalSeats}</td>
                <td className="td"><StatusBadge status={b.status} /></td>
                <td className="td">
                  <button onClick={() => openEdit(b)} aria-label="Edit bus" className="text-blue-600 hover:text-blue-800 p-1"><FiEdit2 size={14} /></button>
                </td>
              </tr>
            ))}
            {buses.length === 0 && <tr><td colSpan={6} className="td text-center text-gray-400">No buses yet</td></tr>}
          </tbody>
        </table>
      </div>
      <Modal isOpen={modal} title={editId ? 'Edit Bus' : 'New Bus'} onClose={() => setModal(false)}>
        <form onSubmit={handleSave} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            {!editId && (
              <>
                <div><label className="block text-xs font-medium text-gray-600 mb-1">Bus Number</label>
                  <input required className="input-field text-sm" value={form.busNumber} onChange={upd('busNumber')} /></div>
                <div><label className="block text-xs font-medium text-gray-600 mb-1">Registration No</label>
                  <input required className="input-field text-sm" value={form.registrationNo} onChange={upd('registrationNo')} /></div>
                <div><label className="block text-xs font-medium text-gray-600 mb-1">Total Seats</label>
                  <input required type="number" min="10" max="100" className="input-field text-sm" value={form.totalSeats} onChange={upd('totalSeats')} /></div>
              </>
            )}
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Bus Type</label>
              <select className="input-field text-sm" value={form.busType} onChange={upd('busType')}>
                {['AC Sleeper', 'AC Semi-Sleeper', 'AC Seater', 'Non-AC Seater', 'Non-AC Sleeper'].map(t => <option key={t}>{t}</option>)}
              </select></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Status</label>
              <select className="input-field text-sm" value={form.status} onChange={upd('status')}>
                <option>ACTIVE</option><option>MAINTENANCE</option><option>INACTIVE</option>
              </select></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Mfg Year</label>
              <input type="number" min="2000" max="2030" className="input-field text-sm" value={form.manufacturingYear} onChange={upd('manufacturingYear')} /></div>
          </div>
          <div className="flex gap-3 justify-end pt-2">
            <button type="button" onClick={() => setModal(false)} className="btn-secondary text-sm py-1.5 px-4">Cancel</button>
            <button type="submit" disabled={submitting} className="btn-primary text-sm py-1.5 px-4">{editId ? 'Update' : 'Create'}</button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
