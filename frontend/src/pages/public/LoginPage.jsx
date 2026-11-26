import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { authApi } from '../../api'
import toast from 'react-hot-toast'
import { FiMail, FiLock, FiTruck, FiArrowRight } from 'react-icons/fi'

export default function LoginPage() {
  const [form, setForm]       = useState({ email: '', password: '' })
  const [loading, setLoading] = useState(false)
  const { login }             = useAuth()
  const navigate              = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      const res  = await authApi.login(form)
      const data = res.data.data

      login(data)
      toast.success('Welcome back!')
      navigate(data.roles?.includes('ROLE_ADMIN') ? '/admin' : '/dashboard')
    } catch (err) {
      toast.error(err.response?.data?.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-[80vh] flex animate-fade-in"
      style={{ background: 'linear-gradient(135deg, #06101f 0%, #0c1929 50%, #132238 100%)' }}>

      {/* Left panel — branding */}
      <div className="hidden lg:flex flex-col justify-center px-16 flex-1">
        <div className="flex items-center gap-4 mb-10">
          <div className="w-12 h-12 rounded-2xl bg-gold-gradient flex items-center justify-center shadow-gold">
            <FiTruck size={24} className="text-white" />
          </div>
          <div>
            <div className="text-white text-xl font-extrabold">Karnataka Travels</div>
            <div className="text-gold-400 text-xs font-semibold tracking-widest uppercase">Office</div>
          </div>
        </div>
        <h1 className="text-4xl font-extrabold text-white leading-tight mb-4">
          South India's<br />
          <span className="text-gradient-gold">Smartest Transit</span>
        </h1>
        <p className="text-navy-300 text-base leading-relaxed max-w-sm">
          Connecting 10 cities across Tamil Nadu, Karnataka, Kerala &amp; Andhra Pradesh with seamless bus travel.
        </p>
        <div className="flex gap-8 mt-10">
          {[{ v: '19', l: 'Routes' }, { v: '4', l: 'States' }, { v: '36K+', l: 'Passengers' }].map(s => (
            <div key={s.l}>
              <div className="text-2xl font-extrabold text-white">{s.v}</div>
              <div className="text-navy-400 text-xs font-medium">{s.l}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Right panel — form */}
      <div className="w-full lg:w-[440px] flex items-center justify-center px-6 py-10">
        <div className="w-full max-w-sm">
          <div className="bg-white rounded-3xl p-8 shadow-modal animate-slide-up">
            <div className="text-center mb-8">
              <div className="w-12 h-12 rounded-2xl bg-gold-gradient flex items-center justify-center mx-auto mb-4 shadow-gold lg:hidden">
                <FiTruck size={22} className="text-white" />
              </div>
              <h2 className="text-2xl font-extrabold text-navy-900">Welcome back</h2>
              <p className="text-navy-400 text-sm mt-1">Sign in to your account</p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="form-label">Email</label>
                <div className="relative">
                  <FiMail className="absolute left-3.5 top-1/2 -translate-y-1/2 text-navy-400" size={15} />
                  <input type="email" required value={form.email}
                    onChange={e => setForm({ ...form, email: e.target.value })}
                    className="input-field pl-10" placeholder="you@email.com" />
                </div>
              </div>
              <div>
                <label className="form-label">Password</label>
                <div className="relative">
                  <FiLock className="absolute left-3.5 top-1/2 -translate-y-1/2 text-navy-400" size={15} />
                  <input type="password" required value={form.password}
                    onChange={e => setForm({ ...form, password: e.target.value })}
                    className="input-field pl-10" placeholder="••••••••" />
                </div>
              </div>
              <button type="submit" disabled={loading} className="btn-primary w-full py-3 mt-2 gap-2">
                {loading ? 'Signing in...' : 'Sign In'}
                {!loading && <FiArrowRight size={16} />}
              </button>
            </form>

            <p className="text-center text-sm text-navy-400 mt-5">
              No account?{' '}
              <Link to="/register" className="text-navy-700 font-semibold hover:text-gold-600 transition-colors">
                Register free
              </Link>
            </p>

          </div>
        </div>
      </div>
    </div>
  )
}
