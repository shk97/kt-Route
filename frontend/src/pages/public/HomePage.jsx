import { Link } from 'react-router-dom'
import { FiTruck, FiMap, FiCalendar, FiShield, FiTrendingUp, FiStar, FiArrowRight, FiMapPin } from 'react-icons/fi'

const features = [
  { icon: FiTruck,       title: 'Easy Bus Search',     desc: 'Find buses by route, date, and destination in seconds.',                color: 'text-blue-500',   bg: 'bg-blue-50' },
  { icon: FiCalendar,    title: 'Online Booking',       desc: 'Book tickets online with seat selection and instant confirmation.',     color: 'text-emerald-500', bg: 'bg-emerald-50' },
  { icon: FiShield,      title: 'Secure Payments',      desc: 'Safe, encrypted booking with BCrypt-protected accounts.',              color: 'text-purple-500',  bg: 'bg-purple-50' },
  { icon: FiMap,         title: 'Route Tracking',       desc: 'View schedules, stops, and real-time route information.',              color: 'text-gold-600',    bg: 'bg-gold-50' },
  { icon: FiTrendingUp,  title: 'Analytics Dashboard',  desc: 'Admins get powerful analytics to optimize routes and capacity.',       color: 'text-red-500',     bg: 'bg-red-50' },
  { icon: FiStar,        title: 'Premium Service',      desc: 'Multiple bus types from AC Sleeper to economy seater.',               color: 'text-navy-600',    bg: 'bg-navy-50' },
]

const routes = [
  { from: 'Chennai', to: 'Bengaluru', duration: '6h', price: '₹800' },
  { from: 'Bengaluru', to: 'Mysore', duration: '3h', price: '₹450' },
  { from: 'Chennai', to: 'Kochi', duration: '9h', price: '₹1200' },
  { from: 'Vijayawada', to: 'Chennai', duration: '7h', price: '₹950' },
]

export default function HomePage() {
  return (
    <div className="animate-fade-in">
      {/* ── Hero ─────────────────────────────────────────────── */}
      <section className="relative overflow-hidden" style={{
        background: 'linear-gradient(135deg, #06101f 0%, #0c1929 40%, #132238 70%, #1a2d4a 100%)',
        minHeight: '580px'
      }}>
        {/* Decorative orbs */}
        <div className="absolute -top-32 -right-32 w-96 h-96 rounded-full opacity-10"
          style={{ background: 'radial-gradient(circle, #f59e0b, transparent)' }} />
        <div className="absolute -bottom-20 -left-20 w-72 h-72 rounded-full opacity-8"
          style={{ background: 'radial-gradient(circle, #2b4a7a, transparent)' }} />

        <div className="relative max-w-5xl mx-auto px-6 py-24 text-center">
          {/* Eyebrow */}
          <div className="inline-flex items-center gap-2 bg-white/8 border border-white/15 text-gold-400 text-xs font-semibold tracking-widest uppercase px-4 py-2 rounded-full mb-8">
            <span className="live-dot" />
            State Road Transportation
          </div>

          <h1 className="text-5xl md:text-6xl font-extrabold text-white mb-6 leading-tight tracking-tight">
            Karnataka Travels<br />
            <span className="text-gradient-gold">Office</span>
          </h1>

          <p className="text-lg text-navy-300 mb-10 max-w-2xl mx-auto leading-relaxed">
            Connecting Tamil Nadu, Karnataka, Kerala &amp; Andhra Pradesh —
            smarter journeys, better service across South India.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link to="/search"
              className="btn-primary text-base px-8 py-3.5 shadow-gold">
              <FiSearch size={18} />
              Search Buses
            </Link>
            <Link to="/register"
              className="inline-flex items-center justify-center gap-2 bg-white/10 border border-white/20 text-white px-8 py-3.5 rounded-xl font-semibold text-base hover:bg-white/15 transition-all">
              Create Account
              <FiArrowRight size={16} />
            </Link>
          </div>

          {/* Stats row */}
          <div className="flex flex-wrap justify-center gap-8 mt-16">
            {[
              { value: '19', label: 'Active Routes' },
              { value: '10', label: 'Cities Connected' },
              { value: '4', label: 'States Covered' },
              { value: '36K+', label: 'Passengers Served' },
            ].map(s => (
              <div key={s.label} className="text-center">
                <div className="text-2xl font-extrabold text-white">{s.value}</div>
                <div className="text-navy-400 text-xs font-medium mt-0.5">{s.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Popular Routes ────────────────────────────────────── */}
      <section className="py-16 px-6" style={{ background: '#f0f2f7' }}>
        <div className="max-w-5xl mx-auto">
          <div className="text-center mb-10">
            <h2 className="text-2xl font-bold text-navy-900">Popular Routes</h2>
            <p className="text-navy-400 text-sm mt-2">Most travelled corridors across South India</p>
          </div>
          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {routes.map((r, i) => (
              <Link key={i} to="/search"
                className="card-hover group cursor-pointer">
                <div className="flex items-start justify-between mb-4">
                  <div className="w-9 h-9 rounded-xl bg-navy-50 flex items-center justify-center">
                    <FiMapPin size={16} className="text-navy-600" />
                  </div>
                  <span className="text-xs font-semibold text-gold-600 bg-gold-50 px-2.5 py-1 rounded-full">{r.duration}</span>
                </div>
                <div className="text-navy-900 font-bold text-base">{r.from}</div>
                <div className="flex items-center gap-1 my-1">
                  <div className="flex-1 border-t border-dashed border-navy-200" />
                  <FiArrowRight size={12} className="text-navy-400" />
                  <div className="flex-1 border-t border-dashed border-navy-200" />
                </div>
                <div className="text-navy-700 font-semibold text-sm">{r.to}</div>
                <div className="mt-3 text-lg font-extrabold text-navy-900">
                  {r.price} <span className="text-xs font-normal text-navy-400">onwards</span>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* ── Features ──────────────────────────────────────────── */}
      <section className="py-16 px-6 bg-white">
        <div className="max-w-5xl mx-auto">
          <div className="text-center mb-10">
            <h2 className="text-2xl font-bold text-navy-900">Why Choose Karnataka Travels?</h2>
            <p className="text-navy-400 text-sm mt-2">Everything you need for a seamless journey</p>
          </div>
          <div className="grid md:grid-cols-3 gap-5">
            {features.map((f, i) => (
              <div key={i} className="card-hover group">
                <div className={`w-11 h-11 ${f.bg} rounded-2xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform`}>
                  <f.icon size={22} className={f.color} />
                </div>
                <h3 className="font-bold text-navy-900 mb-2">{f.title}</h3>
                <p className="text-sm text-navy-500 leading-relaxed">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA ───────────────────────────────────────────────── */}
      <section className="py-16 px-6 text-center"
        style={{ background: 'linear-gradient(135deg, #06101f 0%, #0c1929 60%, #132238 100%)' }}>
        <div className="max-w-2xl mx-auto">
          <h2 className="text-3xl font-bold text-white mb-3">Ready to Travel?</h2>
          <p className="text-navy-300 mb-8">
            Join thousands of passengers on the Karnataka Travels network
          </p>
          <Link to="/search" className="btn-primary text-base px-10 py-3.5">
            Book Your Journey
            <FiArrowRight size={18} />
          </Link>
        </div>
      </section>
    </div>
  )
}

function FiSearch({ size }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
    </svg>
  )
}
