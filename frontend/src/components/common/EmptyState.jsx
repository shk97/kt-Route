export default function EmptyState({ title = 'No data found', message, icon: Icon }) {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      {Icon && (
        <div className="w-16 h-16 bg-navy-50 rounded-3xl flex items-center justify-center mb-5">
          <Icon size={32} className="text-navy-300" />
        </div>
      )}
      <h3 className="text-base font-bold text-navy-700">{title}</h3>
      {message && <p className="text-sm text-navy-400 mt-1.5 max-w-xs">{message}</p>}
    </div>
  )
}
