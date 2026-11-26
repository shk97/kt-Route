import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { FiTruck, FiUser, FiLogOut, FiLogIn, FiSearch } from 'react-icons/fi'

export default function Navbar() {
  const { user, logout, isAdmin } = useAuth()
  const navigate = useNavigate()
  const handleLogout = () => { logout(); navigate('/') }

  return (
    <nav className="relative z-50" style={{ background: 'linear-gradient(90deg, #06101f 0%, #0c1929 50%, #132238 100%)', boxShadow: '0 2px 20px rgba(0,0,0,0.3)' }}>
      <div className="max-w-7xl mx-auto px-6 py-0 flex items-center justify-between h-16">
        {/* Brand */}
        <Link to="/" className="flex items-center gap-3 group">
          <div className="w-9 h-9 rounded-xl bg-gold-gradient flex items-center justify-center shadow-gold group-hover:brightness-110 transition-all">
            <FiTruck size={18} className="text-white" />
          </div>
          <div className="leading-tight">
            <div className="text-white text-sm font-bold tracking-wide">Karnataka Travels</div>
            <div className="text-gold-400 text-[10px] font-semibold tracking-widest uppercase">Office</div>
          </div>
        </Link>

        {/* Nav links */}
        <div className="flex items-center gap-1 text-sm">
          <Link to="/search"
            className="flex items-center gap-1.5 text-navy-300 hover:text-white px-4 py-2 rounded-lg hover:bg-white/8 transition-all font-medium">
            <FiSearch size={14} />
            <span>Search Buses</span>
          </Link>

          {user ? (
            <>
              {isAdmin() ? (
                <Link to="/admin"
                  className="text-navy-300 hover:text-white px-4 py-2 rounded-lg hover:bg-white/8 transition-all font-medium">
                  Admin Panel
                </Link>
              ) : (
                <Link to="/my-bookings"
                  className="text-navy-300 hover:text-white px-4 py-2 rounded-lg hover:bg-white/8 transition-all font-medium">
                  My Bookings
                </Link>
              )}
              <div className="flex items-center gap-2 ml-2 pl-4 border-l border-white/10">
                <div className="flex items-center gap-2 text-navy-200 bg-white/8 px-3 py-1.5 rounded-full text-xs font-medium">
                  <div className="w-5 h-5 rounded-full bg-navy-600 flex items-center justify-center">
                    <FiUser size={10} className="text-navy-200" />
                  </div>
                  <span>{user.fullName}</span>
                </div>
                <button onClick={handleLogout}
                  className="flex items-center gap-1 text-navy-400 hover:text-red-400 px-2 py-1.5 rounded-lg hover:bg-white/8 transition-all text-xs font-medium">
                  <FiLogOut size={14} /><span>Logout</span>
                </button>
              </div>
            </>
          ) : (
            <div className="flex items-center gap-2 ml-2 pl-4 border-l border-white/10">
              <Link to="/login"
                className="flex items-center gap-1.5 text-navy-300 hover:text-white px-4 py-2 rounded-lg hover:bg-white/8 transition-all font-medium">
                <FiLogIn size={14} /><span>Login</span>
              </Link>
              <Link to="/register"
                className="btn-primary text-sm px-5 py-2">
                Register
              </Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  )
}
