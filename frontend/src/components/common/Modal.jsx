import { FiX } from 'react-icons/fi'

export default function Modal({ isOpen, title, onClose, children, size = 'md' }) {
  if (!isOpen) return null
  const sizeClass = { sm: 'max-w-sm', md: 'max-w-lg', lg: 'max-w-2xl', xl: 'max-w-4xl' }[size] || 'max-w-lg'
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto animate-fade-in"
      style={{ background: 'rgba(6,16,31,0.6)', backdropFilter: 'blur(4px)' }}>
      <div className={`bg-white rounded-2xl shadow-modal w-full ${sizeClass} my-8 animate-slide-up`}>
        <div className="flex items-center justify-between px-6 py-4 border-b border-navy-100/60">
          <h2 className="text-base font-bold text-navy-900">{title}</h2>
          <button onClick={onClose} aria-label="Close"
            className="p-1.5 hover:bg-navy-50 rounded-xl transition-colors text-navy-400 hover:text-navy-700">
            <FiX size={18} />
          </button>
        </div>
        <div className="p-6">{children}</div>
      </div>
    </div>
  )
}
