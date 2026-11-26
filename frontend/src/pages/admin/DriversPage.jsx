import { useEffect, useState } from 'react'
import { driverApi } from '../../api'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import StatusBadge from '../../components/common/StatusBadge'
import Modal from '../../components/common/Modal'
import toast from 'react-hot-toast'
import { FiPlus, FiEdit2 } from 'react-icons/fi'

const EMPTY = { fullName: '', licenseNo: '', phone: '', email: '' }

export default function DriversPage() {
  const [drivers, setDrivers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [modal, setModal] = useState(false)
  const [editId, setEditId] = useState(null)
  const [form, setForm] = useState(EMPTY)

  const load = () => driverApi.getAll().then(r => setDrivers(r.data.data || []))
  useEffect(() => {
    load()
      .catch(() => setError('Failed to load drivers'))
      .finally(() => setLoading(false))
  }, [])

  const openCreate = () => { setForm(EMPTY); setEditId(null); setModal(true) }
  const openEdit = (d) => { setForm({ fullName: d.fullName, licenseNo: d.licenseNo, phone: d.phone, email: d.email || '' }); setEditId(d.id); setModal(true) }

  const handleSave = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    try {
      if (editId) await driverApi.update(editId, form)
      else await driverApi.create(form)
      toast.success(`Driver ${editId ? 'updated' : 'created'}`)
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
        <h1 className="text-xl font-bold text-gray-800">Driver Management</h1>
        <button onClick={openCreate} className="btn-primary flex items-center gap-2 text-sm"><FiPlus size={14} />Add Driver</button>
      </div>
      <div className="table-container">
        <table className="table-base">
          <thead><tr>
            <th className="th">Name</th><th className="th">License No</th>
            <th className="th">Phone</th><th className="th">Status</th><th className="th">Actions</th>
          </tr></thead>
          <tbody className="divide-y divide-gray-100">
            {drivers.map(d => (
              <tr key={d.id} className="hover:bg-gray-50">
                <td className="td font-medium">{d.fullName}</td>
                <td className="td font-mono text-xs">{d.licenseNo}</td>
                <td className="td text-xs">{d.phone}</td>
                <td className="td"><StatusBadge status={d.isActive ? 'ACTIVE' : 'INACTIVE'} /></td>
                <td className="td">
                  <button onClick={() => openEdit(d)} aria-label="Edit driver" className="text-blue-600 hover:text-blue-800 p-1"><FiEdit2 size={14} /></button>
                </td>
              </tr>
            ))}
            {drivers.length === 0 && <tr><td colSpan={5} className="td text-center text-gray-400">No drivers yet</td></tr>}
          </tbody>
        </table>
      </div>
      <Modal isOpen={modal} title={editId ? 'Edit Driver' : 'New Driver'} onClose={() => setModal(false)}>
        <form onSubmit={handleSave} className="space-y-3">
          {[{ k: 'fullName', l: 'Full Name', r: true }, { k: 'licenseNo', l: 'License Number', r: true }, { k: 'phone', l: 'Phone', r: true }, { k: 'email', l: 'Email (optional)', r: false }].map(f => (
            <div key={f.k}>
              <label className="block text-xs font-medium text-gray-600 mb-1">{f.l}</label>
              <input className="input-field text-sm" required={f.r} value={form[f.k]} onChange={upd(f.k)} />
            </div>
          ))}
          <div className="flex gap-3 justify-end pt-2">
            <button type="button" onClick={() => setModal(false)} className="btn-secondary text-sm py-1.5 px-4">Cancel</button>
            <button type="submit" disabled={submitting} className="btn-primary text-sm py-1.5 px-4">{editId ? 'Update' : 'Create'}</button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
