export default function LoadingSpinner({ message = 'Loading...' }) {
  return (
    <div className="flex flex-col items-center justify-center py-20 gap-4">
      <div className="relative w-12 h-12">
        <div className="absolute inset-0 rounded-full border-4 border-navy-100" />
        <div className="absolute inset-0 rounded-full border-4 border-transparent border-t-gold-500 animate-spin" />
      </div>
      <p className="text-navy-400 text-sm font-medium">{message}</p>
    </div>
  )
}
