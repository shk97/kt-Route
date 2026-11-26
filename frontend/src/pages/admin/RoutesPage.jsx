import { useEffect, useState } from 'react'
import { routeApi, stationApi } from '../../api'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import StatusBadge from '../../components/common/StatusBadge'
import Modal from '../../components/common/Modal'
import ConfirmDialog from '../../components/common/ConfirmDialog'
import toast from 'react-hot-toast'
import { FiPlus, FiEdit2, FiTrash2 } from 'react-icons/fi'

const EMPTY = { name:'', routeCode:'', startStationId:'', endStationId:'', distanceKm:'', estimatedDurationMinutes:'', status:'ACTIVE', description:'' }

export default function RoutesPage() {
  const [routes, setRoutes] = useState([])
  const [stations, setStations] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [modal, setModal] = useState(false)
  const [editId, setEditId] = useState(null)
  const [form, setForm] = useState(EMPTY)
  const [confirmId, setConfirmId] = useState(null)

  const load = () => routeApi.getAllAdmin().then(r => setRoutes(r.data.data || []))

  useEffect(() => {
    Promise.all([load(), stationApi.getAll().then(r => setStations(r.data.data || []))])
      .catch(() => setError('Failed to load routes data'))
      .finally(() => setLoading(false))
  }, [])

  const openCreate = () => { setForm(EMPTY); setEditId(null); setModal(true) }
  const openEdit = (r) => {
    setForm({ name:r.name, routeCode:r.routeCode, startStationId:r.startStation?.id||'', endStationId:r.endStation?.id||'', distanceKm:r.distanceKm, estimatedDurationMinutes:r.estimatedDurationMinutes, status:r.status, description:r.description||'' })
    setEditId(r.id); setModal(true)
  }

  const handleSave = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    try {
      const payload = { ...form, startStationId: Number(form.startStationId), endStationId: Number(form.endStationId), distanceKm: Number(form.distanceKm), estimatedDurationMinutes: Number(form.estimatedDurationMinutes) }
      if (editId) await routeApi.update(editId, payload)
      else await routeApi.create(payload)
      toast.success(`Route ${editId ? 'updated' : 'created'}`)
      setModal(false); load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Save failed')
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async () => {
    try { await routeApi.deactivate(confirmId); toast.success('Route deactivated'); load() }
    catch { toast.error('Deactivation failed') }
    finally { setConfirmId(null) }
  }

  const upd = (k) => (e) => setForm({...form, [k]: e.target.value})

  if (loading) return <LoadingSpinner />
  if (error) return <div className="text-red-500 text-center py-12">{error}</div>

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-bold text-gray-800">Route Management</h1>
        <button onClick={openCreate} className="btn-primary flex items-center gap-2 text-sm"><FiPlus size={14}/>Add Route</button>
      </div>
      <div className="table-container">
        <table className="table-base">
          <thead><tr>
            <th className="th">Code</th><th className="th">Name</th><th className="th">From</th>
            <th className="th">To</th><th className="th">Distance</th><th className="th">Status</th><th className="th">Actions</th>
          </tr></thead>
          <tbody className="divide-y divide-gray-100">
            {routes.map(r => (
              <tr key={r.id} className="hover:bg-gray-50">
                <td className="td font-mono text-xs">{r.routeCode}</td>
                <td className="td font-medium">{r.name}</td>
                <td className="td text-xs">{r.startStation?.name}</td>
                <td className="td text-xs">{r.endStation?.name}</td>
                <td className="td text-xs">{r.distanceKm} km</td>
                <td className="td"><StatusBadge status={r.status} /></td>
                <td className="td">
                  <div className="flex gap-2">
                    <button onClick={() => openEdit(r)} aria-label="Edit route" className="text-blue-600 hover:text-blue-800 p-1"><FiEdit2 size={14}/></button>
                    <button onClick={() => setConfirmId(r.id)} aria-label="Deactivate route" className="text-red-500 hover:text-red-700 p-1"><FiTrash2 size={14}/></button>
                  </div>
                </td>
              </tr>
            ))}
            {routes.length === 0 && <tr><td colSpan={7} className="td text-center text-gray-400">No routes yet</td></tr>}
          </tbody>
        </table>
      </div>

      <Modal isOpen={modal} title={editId ? 'Edit Route' : 'New Route'} onClose={() => setModal(false)}>
        <form onSubmit={handleSave} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Route Name</label>
              <input required className="input-field text-sm" value={form.name} onChange={upd('name')} /></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Route Code</label>
              <input required className="input-field text-sm" value={form.routeCode} onChange={upd('routeCode')} /></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Start Station</label>
              <select required className="input-field text-sm" value={form.startStationId} onChange={upd('startStationId')}>
                <option value="">Select</option>{stations.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}
              </select></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">End Station</label>
              <select required className="input-field text-sm" value={form.endStationId} onChange={upd('endStationId')}>
                <option value="">Select</option>{stations.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}
              </select></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Distance (km)</label>
              <input required type="number" min="1" className="input-field text-sm" value={form.distanceKm} onChange={upd('distanceKm')} /></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Duration (min)</label>
              <input required type="number" min="1" className="input-field text-sm" value={form.estimatedDurationMinutes} onChange={upd('estimatedDurationMinutes')} /></div>
          </div>
          <div><label className="block text-xs font-medium text-gray-600 mb-1">Status</label>
            <select className="input-field text-sm" value={form.status} onChange={upd('status')}>
              <option>ACTIVE</option><option>INACTIVE</option><option>SUSPENDED</option>
            </select></div>
          <div className="flex gap-3 justify-end pt-2">
            <button type="button" onClick={() => setModal(false)} className="btn-secondary text-sm py-1.5 px-4">Cancel</button>
            <button type="submit" disabled={submitting} className="btn-primary text-sm py-1.5 px-4">{editId ? 'Update' : 'Create'}</button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog isOpen={!!confirmId} title="Deactivate Route?" danger
        message="This will deactivate the route. Historical data is preserved."
        onConfirm={handleDelete} onCancel={() => setConfirmId(null)} confirmText="Deactivate" />
    </div>
  )
}
