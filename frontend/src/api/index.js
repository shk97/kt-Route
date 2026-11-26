import api from './axiosConfig'

export const authApi = {
  register:            (data)     => api.post('/api/auth/register', data),
  login:               (data)     => api.post('/api/auth/login', data),
  me:                  ()         => api.get('/api/auth/me'),
  logout:              ()         => api.post('/api/auth/logout'),
  // 2FA login step
  verify2fa:           (data)     => api.post('/api/auth/verify-2fa', data),
  recoveryLogin:       (data)     => api.post('/api/auth/2fa/recovery', data),
  // 2FA enrollment (setup token or full admin token)
  startEnrollment:     ()         => api.get('/api/auth/2fa/enroll'),
  confirmEnrollment:   (data)     => api.post('/api/auth/2fa/confirm-enrollment', data),
  // 2FA management (full admin token required)
  disable2fa:          (data)     => api.post('/api/auth/2fa/disable', data),
}

export const stationApi = {
  getAll:    ()         => api.get('/api/stations'),
  getById:   (id)       => api.get(`/api/stations/${id}`),
  create:    (data)     => api.post('/api/admin/stations', data),
  update:    (id, data) => api.put(`/api/admin/stations/${id}`, data),
  deactivate:(id)       => api.delete(`/api/admin/stations/${id}`),
}

export const routeApi = {
  getAll:    ()         => api.get('/api/routes'),
  getById:   (id)       => api.get(`/api/routes/${id}`),
  getAllAdmin:()         => api.get('/api/admin/routes'),
  create:    (data)     => api.post('/api/admin/routes', data),
  update:    (id, data) => api.put(`/api/admin/routes/${id}`, data),
  deactivate:(id)       => api.delete(`/api/admin/routes/${id}`),
}

export const busApi = {
  getAll:      ()         => api.get('/api/admin/buses'),
  getById:     (id)       => api.get(`/api/admin/buses/${id}`),
  getAvailable:(s, e)     => api.get(`/api/admin/buses/available?start=${s}&end=${e}`),
  create:      (data)     => api.post('/api/admin/buses', data),
  update:      (id, data) => api.put(`/api/admin/buses/${id}`, data),
  deactivate:  (id)       => api.delete(`/api/admin/buses/${id}`),
}

export const driverApi = {
  getAll:      ()         => api.get('/api/admin/drivers'),
  getAvailable:(s, e)     => api.get(`/api/admin/drivers/available?start=${s}&end=${e}`),
  create:      (data)     => api.post('/api/admin/drivers', data),
  update:      (id, data) => api.put(`/api/admin/drivers/${id}`, data),
  deactivate:  (id)       => api.delete(`/api/admin/drivers/${id}`),
}

export const tripApi = {
  search:    (startId, endId, date) => api.get(`/api/trips/search?startStationId=${startId}&endStationId=${endId}&date=${date}`),
  getById:   (id)       => api.get(`/api/trips/${id}`),
  getAll:    ()         => api.get('/api/admin/trips'),
  getByRoute:(routeId)  => api.get(`/api/admin/trips/route/${routeId}`),
  create:    (data)     => api.post('/api/admin/trips', data),
  update:    (id, data) => api.put(`/api/admin/trips/${id}`, data),
  cancel:    (id, reason) => api.post(`/api/admin/trips/${id}/cancel`, { reason }),
}

export const bookingApi = {
  create:    (data)     => api.post('/api/bookings', data),
  getMyBookings: ()     => api.get('/api/bookings/my'),
  getById:   (id)       => api.get(`/api/bookings/${id}`),
  getByRef:  (ref)      => api.get(`/api/bookings/ref/${ref}`),
  cancel:    (id, reason) => api.post(`/api/bookings/${id}/cancel`, { reason }),
  getAll:    ()         => api.get('/api/bookings/admin/all'),
  getByTrip: (tripId)   => api.get(`/api/bookings/admin/trip/${tripId}`),
}

export const analyticsApi = {
  dashboard:      ()          => api.get('/api/admin/analytics/dashboard'),
  routeDemand:    (from, to, sortBy = 'passengers_desc') => {
    const params = new URLSearchParams({ sortBy })
    if (from) { params.set('from', from); params.set('to', to) }
    return api.get(`/api/admin/analytics/routes?${params}`)
  },
  stationActivity:(from, to)  => api.get(`/api/admin/analytics/stations${from ? `?from=${from}&to=${to}` : ''}`),
  capacity:       (from, to)  => api.get(`/api/admin/analytics/capacity${from ? `?from=${from}&to=${to}` : ''}`),
  monthly:        (fy, ty)    => api.get(`/api/admin/analytics/monthly?fromYear=${fy}&toYear=${ty}`),
  monthlyByRoute: (routeId, fy, ty) => api.get(`/api/admin/analytics/monthly/route/${routeId}?fromYear=${fy}&toYear=${ty}`),
  profitability:  (from, to)  => api.get(`/api/admin/analytics/profitability${from ? `?from=${from}&to=${to}` : ''}`),

  // Station boarding analytics
  stationOverview:  (params = '') => api.get(`/api/admin/analytics/stations/overview${params}`),
  stationRanked:    (params = '') => api.get(`/api/admin/analytics/stations/ranked${params}`),
  stationRoutes:    (id, params = '') => api.get(`/api/admin/analytics/stations/${id}/routes${params}`),
  stationTripDetails:(id, routeId, params = '') => api.get(`/api/admin/analytics/stations/${id}/routes/${routeId}/trips${params}`),
  stationPeakHours: (id, params = '') => api.get(`/api/admin/analytics/stations/${id}/peak-hours${params}`),
  stationTrends:    (params = '') => api.get(`/api/admin/analytics/stations/trends${params}`),
  stationMap:       (params = '') => api.get(`/api/admin/analytics/stations/map${params}`),
  stationRecommendations: (params = '') => api.get(`/api/admin/analytics/stations/recommendations${params}`),
}

export const paymentApi = {
  initiate:  (data)                       => api.post('/api/payments/initiate', data),
  confirm:   (paymentRef, razorpayData)   => api.post(`/api/payments/${paymentRef}/confirm`, razorpayData),
  fail:      (paymentRef, reason)         => api.post(`/api/payments/${paymentRef}/fail`, { reason }),
  getByRef:  (paymentRef)                 => api.get(`/api/payments/${paymentRef}`),
}

export const reportApi = {
  routes:        (format, from, to) => api.get(`/api/admin/reports/routes?format=${format}${from ? `&from=${from}&to=${to}` : ''}`, { responseType: 'blob' }),
  monthly:       (format, fy, ty)   => api.get(`/api/admin/reports/monthly?format=${format}&fromYear=${fy}&toYear=${ty}`, { responseType: 'blob' }),
  capacity:      (format, from, to) => api.get(`/api/admin/reports/capacity?format=${format}${from ? `&from=${from}&to=${to}` : ''}`, { responseType: 'blob' }),
  profitability: (format, from, to) => api.get(`/api/admin/reports/profitability?format=${format}${from ? `&from=${from}&to=${to}` : ''}`, { responseType: 'blob' }),
}
