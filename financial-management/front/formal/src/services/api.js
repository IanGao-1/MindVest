const API_PREFIX = '/api'

async function request(path, options = {}) {
  const response = await fetch(`${API_PREFIX}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  })

  if (!response.ok) {
    let message = `Request failed: ${response.status}`
    try {
      const errorBody = await response.json()
      if (errorBody && errorBody.message) {
        message = errorBody.message
      }
    } catch (error) {
      // Ignore JSON parse failures and fall back to status text.
      if (response.statusText) {
        message = response.statusText
      }
    }
    throw new Error(message)
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}

export function fetchAssets() {
  return request('/assets')
}

export function fetchTransactions() {
  return request('/transactions')
}

export function createTransaction(payload) {
  return request('/transactions', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function fetchQuote(ticker) {
  return request(`/market-data/yahoo/${encodeURIComponent(ticker)}`)
}
