import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'

// Public pages
import HomePage        from './pages/public/HomePage'
import LoginPage       from './pages/public/LoginPage'
import RegisterPage    from './pages/public/RegisterPage'
import SearchPage      from './pages/public/SearchPage'
import RouteDetailPage from './pages/public/RouteDetailPage'

// Passenger pages
import PassengerDashboard from './pages/passenger/PassengerDashboard'
import BookingPage        from './pages/passenger/BookingPage'
import BookingConfirm     from './pages/passenger/BookingConfirmPage'
import MyBookings         from './pages/passenger/MyBookingsPage'
import BookingDetail      from './pages/passenger/BookingDetailPage'

// Admin pages
import AdminDashboard      from './pages/admin/AdminDashboard'
import RoutesAdmin         from './pages/admin/RoutesPage'
import StationsAdmin       from './pages/admin/StationsPage'
import BusesAdmin          from './pages/admin/BusesPage'
import DriversAdmin        from './pages/admin/DriversPage'
import SchedulesAdmin      from './pages/admin/SchedulesPage'
import RouteDemandPage     from './pages/admin/RouteDemandPage'
import StationActivityPage from './pages/admin/StationActivityPage'
import CapacityPage        from './pages/admin/CapacityPage'
import MonthlyAnalytics    from './pages/admin/MonthlyAnalyticsPage'
import ProfitabilityPage   from './pages/admin/ProfitabilityPage'
import ReportsPage         from './pages/admin/ReportsPage'

// Layouts
import PublicLayout    from './components/layout/PublicLayout'
import AdminLayout     from './components/layout/AdminLayout'
import PassengerLayout from './components/layout/PassengerLayout'

function ProtectedRoute({ children, role }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="flex items-center justify-center min-h-screen"><div className="spinner" /></div>
  if (!user) return <Navigate to="/login" replace />
  if (role === 'admin' && !user.roles?.includes('ROLE_ADMIN')) return <Navigate to="/dashboard" replace />
  if (role === 'passenger' && !user.roles?.includes('ROLE_PASSENGER')) return <Navigate to="/admin" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      {/* Public */}
      <Route element={<PublicLayout />}>
        <Route path="/"           element={<HomePage />} />
        <Route path="/login"      element={<LoginPage />} />
        <Route path="/register"   element={<RegisterPage />} />
        <Route path="/search"     element={<SearchPage />} />
        <Route path="/routes/:id" element={<RouteDetailPage />} />
      </Route>

      {/* Passenger */}
      <Route element={<ProtectedRoute role="passenger"><PassengerLayout /></ProtectedRoute>}>
        <Route path="/dashboard"         element={<PassengerDashboard />} />
        <Route path="/book/:tripId"      element={<BookingPage />} />
        <Route path="/booking/confirm"   element={<BookingConfirm />} />
        <Route path="/my-bookings"       element={<MyBookings />} />
        <Route path="/my-bookings/:id"   element={<BookingDetail />} />
      </Route>

      {/* Admin */}
      <Route element={<ProtectedRoute role="admin"><AdminLayout /></ProtectedRoute>}>
        <Route path="/admin"                element={<AdminDashboard />} />
        <Route path="/admin/routes"         element={<RoutesAdmin />} />
        <Route path="/admin/stations"       element={<StationsAdmin />} />
        <Route path="/admin/buses"          element={<BusesAdmin />} />
        <Route path="/admin/drivers"        element={<DriversAdmin />} />
        <Route path="/admin/schedules"      element={<SchedulesAdmin />} />
        <Route path="/admin/route-demand"   element={<RouteDemandPage />} />
        <Route path="/admin/stations-activity" element={<StationActivityPage />} />
        <Route path="/admin/capacity"       element={<CapacityPage />} />
        <Route path="/admin/monthly"        element={<MonthlyAnalytics />} />
        <Route path="/admin/profitability"  element={<ProfitabilityPage />} />
        <Route path="/admin/reports"        element={<ReportsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
