export default function ConfirmDialog({ isOpen, title, message, children, onConfirm, onCancel, confirmText = 'Confirm', danger = false }) {
  if (!isOpen) return null
  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
      <div className="bg-white rounded-xl shadow-xl max-w-sm w-full p-6">
        <h3 id="confirm-title" className="text-lg font-semibold text-gray-900 mb-2">{title}</h3>
        <p className="text-sm text-gray-600 mb-4">{message}</p>
        {children && <div className="mb-4">{children}</div>}
        <div className="flex gap-3 justify-end">
          <button onClick={onCancel} className="btn-secondary text-sm py-1.5 px-4">Cancel</button>
          <button onClick={onConfirm} className={`${danger ? 'btn-danger' : 'btn-primary'} text-sm py-1.5 px-4`}>
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  )
}
