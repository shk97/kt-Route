import { useState } from 'react'
import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import {
  FiHome, FiMap, FiMapPin, FiTruck, FiUsers, FiCalendar,
  FiBarChart2, FiActivity, FiPieChart, FiTrendingUp, FiDollarSign,
  FiFileText, FiMenu, FiX, FiLogOut, FiUser, FiChevronRight
} from 'react-icons/fi'

const navItems = [
  { to: '/admin',                   label: 'Dashboard',        icon: FiHome,      exact: true },
  { to: '/admin/routes',            label: 'Routes',            icon: FiMap },
  { to: '/admin/stations',          label: 'Stations',          icon: FiMapPin },
  { to: '/admin/buses',             label: 'Buses',             icon: FiTruck },
  { to: '/admin/drivers',           label: 'Drivers',           icon: FiUsers },
  { to: '/admin/schedules',         label: 'Schedules',         icon: FiCalendar },
  { to: '/admin/route-demand',      label: 'Route Demand',      icon: FiBarChart2 },
  { to: '/admin/stations-activity', label: 'Station Analytics', icon: FiActivity },
  { to: '/admin/capacity',          label: 'Capacity',          icon: FiPieChart },
  { to: '/admin/monthly',           label: 'Monthly Analytics', icon: FiTrendingUp },
  { to: '/admin/profitability',     label: 'Profitability',     icon: FiDollarSign },
  { to: '/admin/reports',           label: 'Reports',           icon: FiFileText },
]

const navGroups = [
  { label: 'Overview',    items: navItems.slice(0, 1) },
  { label: 'Operations',  items: navItems.slice(1, 6) },
  { label: 'Analytics',   items: navItems.slice(6, 11) },
  { label: 'Reporting',   items: navItems.slice(11) },
]

export default function AdminLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const handleLogout = () => { logout(); navigate('/login') }

  return (
    <div className="flex h-screen overflow-hidden" style={{ background: '#f0f2f7' }}>
      {/* ── Sidebar ─────────────────────────────────────────────── */}
      <aside
        className={`${sidebarOpen ? 'w-64' : 'w-[68px]'} flex flex-col flex-shrink-0 transition-all duration-300 ease-in-out relative`}
        style={{ background: 'linear-gradient(180deg, #06101f 0%, #0c1929 60%, #132238 100%)', boxShadow: '4px 0 24px rgba(0,0,0,0.25)' }}
      >
        {/* Logo area */}
        <div className="flex items-center justify-between px-4 py-5 border-b border-white/10">
          {sidebarOpen && (
            <div className="flex items-center gap-3 overflow-hidden">
              <div className="w-8 h-8 rounded-lg bg-gold-gradient flex items-center justify-center flex-shrink-0 shadow-gold">
                <FiTruck size={16} className="text-white" />
              </div>
              <div className="leading-tight">
                <div className="text-white text-sm font-bold tracking-wide truncate">Karnataka</div>
                <div className="text-gold-400 text-[10px] font-semibold tracking-widest uppercase">Travels Office</div>
              </div>
            </div>
          )}
          {!sidebarOpen && (
            <div className="w-8 h-8 rounded-lg bg-gold-gradient flex items-center justify-center mx-auto shadow-gold">
              <FiTruck size={16} className="text-white" />
            </div>
          )}
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="p-1.5 rounded-lg text-navy-300 hover:text-white hover:bg-white/10 transition-colors flex-shrink-0"
          >
            {sidebarOpen ? <FiX size={16} /> : <FiMenu size={16} />}
          </button>
        </div>

        {/* Nav groups */}
        <nav className="flex-1 overflow-y-auto py-4 scrollbar-hide">
          {navGroups.map(group => (
            <div key={group.label} className="mb-1">
              {sidebarOpen && (
                <div className="px-4 pt-3 pb-1 text-[10px] font-bold tracking-widest text-navy-400 uppercase">
                  {group.label}
                </div>
              )}
              {group.items.map(item => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.exact}
                  title={!sidebarOpen ? item.label : undefined}
                  className={({ isActive }) =>
                    `flex items-center gap-3 mx-2 px-3 py-2.5 rounded-xl text-sm transition-all duration-150 group ${
                      isActive
                        ? 'bg-gold-gradient text-white font-semibold shadow-gold'
                        : 'text-navy-300 hover:text-white hover:bg-white/8'
                    }`
                  }
                >
                  {({ isActive }) => (
                    <>
                      <item.icon size={17} className={`flex-shrink-0 ${isActive ? 'text-white' : 'text-navy-400 group-hover:text-white'}`} />
                      {sidebarOpen && <span className="truncate">{item.label}</span>}
                      {sidebarOpen && isActive && <FiChevronRight size={13} className="ml-auto flex-shrink-0" />}
                    </>
                  )}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        {/* User footer */}
        <div className="p-3 border-t border-white/10">
          {sidebarOpen ? (
            <div className="flex items-center gap-3 px-2 py-2 rounded-xl hover:bg-white/8 transition-colors">
              <div className="w-8 h-8 rounded-full bg-navy-600 flex items-center justify-center flex-shrink-0">
                <FiUser size={14} className="text-navy-200" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-white text-xs font-semibold truncate">{user?.fullName}</div>
                <div className="text-navy-400 text-[10px] truncate">Administrator</div>
              </div>
              <button onClick={handleLogout} aria-label="Logout" className="text-navy-400 hover:text-red-400 transition-colors p-1">
                <FiLogOut size={15} />
              </button>
            </div>
          ) : (
            <button onClick={handleLogout} aria-label="Logout"
              className="w-full flex items-center justify-center p-2.5 text-navy-400 hover:text-red-400 transition-colors rounded-xl hover:bg-white/8">
              <FiLogOut size={17} />
            </button>
          )}
        </div>
      </aside>

      {/* ── Main area ─────────────────────────────────────────── */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Top header */}
        <header className="bg-white flex items-center justify-between px-6 py-3.5 flex-shrink-0"
          style={{ boxShadow: '0 1px 0 rgba(12,25,41,0.08), 0 2px 12px rgba(12,25,41,0.04)' }}>
          <div>
            <h1 className="text-base font-bold text-navy-900 tracking-tight">RouteSync</h1>
            <p className="text-[11px] text-navy-400">State Road Transportation Admin Panel</p>
          </div>
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-1.5 text-xs text-emerald-600 bg-emerald-50 px-3 py-1.5 rounded-full border border-emerald-200">
              <span className="live-dot" />
              <span className="font-medium">Live</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-navy-600 bg-navy-50 px-3 py-1.5 rounded-full">
              <FiUser size={14} />
              <span className="font-medium">{user?.fullName}</span>
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-6 animate-fade-in">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
