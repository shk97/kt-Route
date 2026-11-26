import axios from 'axios'

const api = axios.create({
  baseURL: '',
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use(config => {
  // Full JWT (passengers, fully-authenticated admins)
  const token = localStorage.getItem('kt_token')
      // Setup token (admin first-login 2FA enrollment flow)
      || sessionStorage.getItem('kt_setup_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('kt_token')
      localStorage.removeItem('kt_user')
      if (window.location.pathname !== '/login') {
        window.dispatchEvent(new Event('auth:logout'))
      }
    }
    return Promise.reject(err)
  }
)

export default api
