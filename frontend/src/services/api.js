const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

let csrfToken = ''
let csrfHeader = 'X-CSRF-TOKEN'

export async function refreshCsrfToken() {
  const response = await fetch(`${API_BASE}/auth/csrf`, {
    method: 'GET',
    credentials: 'include'
  })

  if (!response.ok) {
    throw new Error('Could not initialize secure request.')
  }

  const data = await response.json()

  csrfToken = data.token
  csrfHeader = data.headerName

  return data
}

export async function apiFetch(endpoint, options = {}) {
  const method = (options.method || 'GET').toUpperCase()
  const requiresCsrf = !['GET', 'HEAD', 'OPTIONS'].includes(method)

  if (requiresCsrf && !csrfToken) {
    await refreshCsrfToken()
  }

  const headers = new Headers(options.headers || {})

  if (requiresCsrf) {
    headers.set(csrfHeader, csrfToken)
  }

  return fetch(`${API_BASE}${endpoint}`, {
    ...options,
    method,
    headers,
    credentials: 'include'
  })
}

export async function loginUser(credentials) {
  await refreshCsrfToken()

  const response = await apiFetch('/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(credentials)
  })

  if (response.ok) {
    await refreshCsrfToken()
  }

  return response
}

export async function logoutUser() {
  try {
    const response = await apiFetch('/auth/logout', {
      method: 'POST'
    })

    if (!response.ok) {
      throw new Error(`Logout failed (HTTP ${response.status}).`)
    }

    return response
  } finally {
    clearCsrfToken()
  }
}

export function clearCsrfToken() {
  csrfToken = ''
  csrfHeader = 'X-CSRF-TOKEN'
}

export { API_BASE }
