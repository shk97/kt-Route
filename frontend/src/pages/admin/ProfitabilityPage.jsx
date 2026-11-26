import { useEffect, useState } from 'react'
import { analyticsApi } from '../../api'
import LoadingSpinner from '../../components/common/LoadingSpinner'

export default function ProfitabilityPage() {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    analyticsApi.profitability().then(r => setData(r.data.data || [])).finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingSpinner />

  return (
    <div>
      <h1 className="text-xl font-bold text-gray-800 mb-6">Revenue &amp; Profitability</h1>
      <div className="table-container">
        <table className="table-base">
          <thead><tr>
            <th className="th">Route</th><th className="th">Passengers</th><th className="th">Revenue</th>
            <th className="th">Operating Cost</th><th className="th">Profit</th><th className="th">Margin</th>
          </tr></thead>
          <tbody className="divide-y divide-gray-100">
            {data.map((r,i) => {
              const profit = r.operatingProfit
              const profitColor = profit == null ? 'text-gray-400' : profit >= 0 ? 'text-green-700' : 'text-red-600'
              return (
                <tr key={i} className="hover:bg-gray-50">
                  <td className="td font-medium text-xs">{r.routeName}</td>
                  <td className="td">{r.passengerCount}</td>
                  <td className="td text-green-700">&#8377;{Number(r.totalRevenue).toLocaleString()}</td>
                  <td className="td">
                    {r.costDataAvailable
                      ? `₹${Number(r.totalOperatingCost).toLocaleString()}`
                      : <span className="text-gray-400 text-xs">N/A</span>}
                  </td>
                  <td className={`td font-semibold ${profitColor}`}>
                    {profit != null ? `₹${Number(profit).toLocaleString()}` : 'N/A'}
                  </td>
                  <td className="td">
                    {r.profitMarginPercent != null
                      ? `${r.profitMarginPercent.toFixed(1)}%`
                      : <span className="text-gray-400 text-xs">N/A</span>}
                  </td>
                </tr>
              )
            })}
            {data.length === 0 && <tr><td colSpan={6} className="td text-center text-gray-400">No data</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  )
}
