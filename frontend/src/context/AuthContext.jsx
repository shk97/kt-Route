import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axiosConfig'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser]         = useState(null)
  const [loading, setLoading]   = useState(true)
  const [authError, setAuthError] = useState(null)
  const navigate = useNavigate()

  const logout = useCallback(() => {
    localStorage.removeItem('kt_token')
    localStorage.removeItem('kt_user')
    setUser(null)
    navigate('/login', { replace: true })
  }, [navigate])

  useEffect(() => {
    window.addEventListener('auth:logout', logout)
    return () => window.removeEventListener('auth:logout', logout)
  }, [logout])

  useEffect(() => {
    const token = localStorage.getItem('kt_token')
    if (!token) { setLoading(false); return }

    const controller = new AbortController()
    api.get('/api/auth/me', { signal: controller.signal })
      .then(res => {
        const data = res.data?.data ?? res.data
        setUser(data)
      })
      .catch(err => {
        if (err.name !== 'CanceledError' && err.name !== 'AbortError') {
          localStorage.removeItem('kt_token')
          localStorage.removeItem('kt_user')
          setAuthError('Session expired. Please log in again.')
        }
      })
      .finally(() => setLoading(false))

    return () => controller.abort()
  }, [])

  const login = (authResponse) => {
    localStorage.setItem('kt_token', authResponse.token)
    localStorage.setItem('kt_user', JSON.stringify(authResponse))
    setUser(authResponse)
    setAuthError(null)
  }

  const isAdmin     = () => user?.roles?.includes('ROLE_ADMIN')     || false
  const isPassenger = () => user?.roles?.includes('ROLE_PASSENGER') || false

  return (
    <AuthContext.Provider value={{ user, login, logout, loading, isAdmin, isPassenger, authError }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
