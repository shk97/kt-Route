const statusMap = {
  ACTIVE:        'badge-green',
  SCHEDULED:     'badge-blue',
  CONFIRMED:     'badge-green',
  COMPLETED:     'badge-gray',
  CANCELLED:     'badge-red',
  PENDING:       'badge-yellow',
  INACTIVE:      'badge-gray',
  MAINTENANCE:   'badge-yellow',
  OVER_CAPACITY: 'badge-red',
  NORMAL:        'badge-green',
  UNDERUTILIZED: 'badge-yellow',
  IN_PROGRESS:   'badge-blue',
  DELAYED:       'badge-yellow',
  SUSPENDED:     'badge-red',
}

export default function StatusBadge({ status }) {
  const cls = statusMap[status] || 'badge-gray'
  return <span className={cls}>{status?.replace(/_/g, ' ')}</span>
}
