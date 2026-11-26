import { useState } from 'react'
import { reportApi } from '../../api'
import toast from 'react-hot-toast'
import { FiDownload } from 'react-icons/fi'

const downloadBlob = (blob, filename) => {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = filename; a.click()
  URL.revokeObjectURL(url)
}

const reports = [
  { key:'routes',        label:'Busiest & Least-Travelled Routes', desc:'All routes ranked by passengers — identifies thick and thin routes',  hasPdf: true },
  { key:'monthly',       label:'Monthly Passenger Demand',         desc:'Month-wise demand with Heavy / Normal / Light classification',        hasPdf: true },
  { key:'capacity',      label:'Capacity Utilization',             desc:'Underutilized, normal, over-capacity trips',                          hasPdf: true },
  { key:'profitability', label:'Route Profitability',              desc:'Revenue vs operating costs — flags Beneficial and Non-Beneficial routes', hasPdf: true },
]

export default function ReportsPage() {
  const [loading, setLoading] = useState({})

  const download = async (key, format) => {
    const loadKey = `${key}_${format}`
    setLoading(l => ({...l, [loadKey]: true}))
    try {
      let res
      if (key === 'monthly') res = await reportApi.monthly(format, 2024, 2026)
      else res = await reportApi[key](format)
      downloadBlob(res.data, `${key}_report.${format}`)
      toast.success('Report downloaded!')
    } catch { toast.error('Download failed') }
    finally { setLoading(l => ({...l, [loadKey]: false})) }
  }

  return (
    <div>
      <h1 className="text-xl font-bold text-gray-800 mb-2">Reports</h1>
      <p className="text-sm text-gray-500 mb-6">Download operational reports in CSV or PDF format</p>
      <div className="grid md:grid-cols-2 gap-4">
        {reports.map(r => (
          <div key={r.key} className="card">
            <h3 className="font-semibold text-gray-800 mb-1">{r.label}</h3>
            <p className="text-xs text-gray-500 mb-4">{r.desc}</p>
            <div className="flex gap-2">
              <button onClick={() => download(r.key, 'csv')} disabled={loading[`${r.key}_csv`]}
                className="btn-secondary text-sm py-1.5 px-4 flex items-center gap-1.5 disabled:opacity-60">
                <FiDownload size={14}/>{loading[`${r.key}_csv`] ? 'Downloading…' : 'CSV'}
              </button>
              {r.hasPdf && (
                <button onClick={() => download(r.key, 'pdf')} disabled={loading[`${r.key}_pdf`]}
                  className="btn-primary text-sm py-1.5 px-4 flex items-center gap-1.5 disabled:opacity-60">
                  <FiDownload size={14}/>{loading[`${r.key}_pdf`] ? 'Downloading…' : 'PDF'}
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
