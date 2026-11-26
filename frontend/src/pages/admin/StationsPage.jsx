import { useEffect, useState } from 'react'
import { stationApi } from '../../api'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import StatusBadge from '../../components/common/StatusBadge'
import Modal from '../../components/common/Modal'
import toast from 'react-hot-toast'
import { FiPlus, FiEdit2 } from 'react-icons/fi'

const EMPTY = { name: '', code: '', location: '', city: '', state: '' }

export default function StationsPage() {
  const [stations, setStations] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [modal, setModal] = useState(false)
  const [editId, setEditId] = useState(null)
  const [form, setForm] = useState(EMPTY)

  const load = () => stationApi.getAll().then(r => setStations(r.data.data || []))
  useEffect(() => {
    load()
      .catch(() => setError('Failed to load stations'))
      .finally(() => setLoading(false))
  }, [])

  const openCreate = () => { setForm(EMPTY); setEditId(null); setModal(true) }
  const openEdit = (s) => { setForm({ name: s.name, code: s.code, location: s.location || '', city: s.city || '', state: s.state || '' }); setEditId(s.id); setModal(true) }

  const handleSave = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    try {
      if (editId) await stationApi.update(editId, form)
      else await stationApi.create(form)
      toast.success(`Station ${editId ? 'updated' : 'created'}`)
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
        <h1 className="text-xl font-bold text-gray-800">Station Management</h1>
        <button onClick={openCreate} className="btn-primary flex items-center gap-2 text-sm"><FiPlus size={14} />Add Station</button>
      </div>
      <div className="table-container">
        <table className="table-base">
          <thead><tr>
            <th className="th">Code</th><th className="th">Name</th><th className="th">City</th>
            <th className="th">State</th><th className="th">Status</th><th className="th">Actions</th>
          </tr></thead>
          <tbody className="divide-y divide-gray-100">
            {stations.map(s => (
              <tr key={s.id} className="hover:bg-gray-50">
                <td className="td font-mono text-xs">{s.code}</td>
                <td className="td font-medium">{s.name}</td>
                <td className="td text-xs">{s.city}</td>
                <td className="td text-xs">{s.state}</td>
                <td className="td"><StatusBadge status={s.isActive ? 'ACTIVE' : 'INACTIVE'} /></td>
                <td className="td">
                  <button onClick={() => openEdit(s)} aria-label="Edit station" className="text-blue-600 hover:text-blue-800 p-1"><FiEdit2 size={14} /></button>
                </td>
              </tr>
            ))}
            {stations.length === 0 && <tr><td colSpan={6} className="td text-center text-gray-400">No stations yet</td></tr>}
          </tbody>
        </table>
      </div>
      <Modal isOpen={modal} title={editId ? 'Edit Station' : 'New Station'} onClose={() => setModal(false)}>
        <form onSubmit={handleSave} className="space-y-3">
          {[{ k: 'name', l: 'Station Name', r: true }, { k: 'code', l: 'Code (e.g. CHN)', r: true }, { k: 'city', l: 'City', r: true }, { k: 'state', l: 'State', r: true }, { k: 'location', l: 'Location Address', r: false }].map(f => (
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
