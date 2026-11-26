const DATE_FMT = new Intl.DateTimeFormat('en-IN', {
  day: '2-digit', month: 'short', year: 'numeric',
  hour: '2-digit', minute: '2-digit', hour12: true,
  timeZone: 'Asia/Kolkata',
})

const DATE_ONLY_FMT = new Intl.DateTimeFormat('en-IN', {
  day: '2-digit', month: 'short', year: 'numeric',
  timeZone: 'Asia/Kolkata',
})

const TIME_ONLY_FMT = new Intl.DateTimeFormat('en-IN', {
  hour: '2-digit', minute: '2-digit', hour12: true,
  timeZone: 'Asia/Kolkata',
})

export function formatDateTime(dt) {
  if (!dt) return '—'
  return DATE_FMT.format(new Date(dt))
}

export function formatDate(dt) {
  if (!dt) return '—'
  return DATE_ONLY_FMT.format(new Date(dt))
}

export function formatTime(dt) {
  if (!dt) return '—'
  return TIME_ONLY_FMT.format(new Date(dt))
}
