import { Outlet } from 'react-router-dom'
import Navbar from './Navbar'
import Footer from './Footer'

export default function PassengerLayout() {
  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />
      <main className="flex-1 py-6">
        <div className="max-w-6xl mx-auto px-4">
          <Outlet />
        </div>
      </main>
      <Footer />
    </div>
  )
}
