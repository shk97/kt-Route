export default function Footer() {
  return (
    <footer style={{ background: 'linear-gradient(90deg, #06101f 0%, #0c1929 100%)' }}
      className="text-center py-5 text-sm">
      <div className="flex flex-col items-center gap-1">
        <p className="text-navy-300 font-medium">
          &copy; {new Date().getFullYear()} RouteSync
        </p>
        <p className="text-navy-500 text-xs">State Road Transportation &mdash; Connecting South India</p>
      </div>
    </footer>
  )
}
