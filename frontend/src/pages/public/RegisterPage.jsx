import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { authApi } from '../../api'
import toast from 'react-hot-toast'
import { FiTruck, FiArrowRight } from 'react-icons/fi'

export default function RegisterPage() {
  const [form, setForm] = useState({
    fullName: '', email: '', phone: '', password: '', confirmPassword: '',
  })
  const [fieldErrors, setFieldErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setFieldErrors({})
    if (form.password !== form.confirmPassword) { toast.error('Passwords do not match'); return }
    setLoading(true)
    try {
      const payload = {
        fullName: form.fullName, email: form.email, phone: form.phone,
        password: form.password, role: 'PASSENGER',
      }
      const res = await authApi.register(payload)
      login(res.data.data)
      toast.success('Account created!')
      navigate('/dashboard')
    } catch (err) {
      const errors = err.response?.data?.errors
      if (errors && typeof errors === 'object') {
        setFieldErrors(errors)
        toast.error('Please fix the errors below')
      } else {
        toast.error(err.response?.data?.message || 'Registration failed')
      }
    } finally { setLoading(false) }
  }

  const update = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const fields = [
    { key: 'fullName',        label: 'Full Name',        type: 'text',     placeholder: 'Ravi Kumar' },
    { key: 'email',           label: 'Email',            type: 'email',    placeholder: 'you@email.com' },
    { key: 'phone',           label: 'Phone Number',     type: 'tel',      placeholder: '9876543210' },
    { key: 'password',        label: 'Password',         type: 'password', placeholder: 'e.g. MyPass@123' },
    { key: 'confirmPassword', label: 'Confirm Password', type: 'password', placeholder: '••••••••' },
  ]

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-10 animate-fade-in"
      style={{ background: 'linear-gradient(135deg, #06101f 0%, #0c1929 50%, #132238 100%)' }}>
      <div className="w-full max-w-md">
        <div className="bg-white rounded-3xl p-8 shadow-modal animate-slide-up">
          {/* Header */}
          <div className="text-center mb-7">
            <div className="w-12 h-12 rounded-2xl bg-gold-gradient flex items-center justify-center mx-auto mb-4 shadow-gold">
              <FiTruck size={22} className="text-white" />
            </div>
            <h2 className="text-2xl font-extrabold text-navy-900">Create Account</h2>
            <p className="text-navy-400 text-sm mt-1">Join Karnataka Travels today</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {fields.map(f => (
              <div key={f.key}>
                <label className="form-label">{f.label}</label>
                <input type={f.type} required value={form[f.key]} onChange={update(f.key)}
                  className={`input-field ${fieldErrors[f.key] ? 'border-red-400' : ''}`}
                  placeholder={f.placeholder} />
                {fieldErrors[f.key] && <p className="text-red-500 text-xs mt-1">{fieldErrors[f.key]}</p>}
                {f.key === 'password' && !fieldErrors[f.key] && (
                  <p className="text-navy-400 text-xs mt-1">Min 8 chars · uppercase · digit · special char</p>
                )}
              </div>
            ))}

            <button type="submit" disabled={loading} className="btn-primary w-full py-3 mt-1 gap-2">
              {loading ? 'Creating Account...' : 'Create Account'}
              {!loading && <FiArrowRight size={16} />}
            </button>
          </form>

          <p className="text-center text-sm text-navy-400 mt-5">
            Already have an account?{' '}
            <Link to="/login" className="text-navy-700 font-semibold hover:text-gold-600 transition-colors">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
