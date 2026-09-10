// Centralized API client: every backend call goes through here so the base
// URL and error handling only need to exist once. VITE_API_BASE_URL is read
// at build time - localhost in dev (.env.development), the real backend
// domain in production (.env.production).
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

async function request(path, options = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })

  if (!response.ok) {
    let message = `Request failed (${response.status})`
    try {
      const body = await response.json()
      if (body?.error) message = body.error
    } catch {
      // Response had no JSON body - fall back to the generic message above.
    }
    throw new Error(message)
  }

  if (response.status === 204) return null
  return response.json()
}

export const api = {
  getItems: (userId) => request(userId ? `/api/items?userId=${userId}` : '/api/items'),
  getRecentItems: (limit = 5) => request(`/api/items/recent?limit=${limit}`),
  getItem: (id) => request(`/api/items/${id}`),
  registerItem: (payload) =>
    request('/api/items', { method: 'POST', body: JSON.stringify(payload) }),

  getPickups: (userId) => request(userId ? `/api/pickups?userId=${userId}` : '/api/pickups'),
  getPendingPickupsByPriority: () => request('/api/pickups/pending'),
  requestPickup: (itemId, pickupLocation, userId) =>
    request('/api/pickups', {
      method: 'POST',
      body: JSON.stringify({ itemId, pickupLocation, userId }),
    }),
  assignRecycler: (pickupId, recyclerCenterId) =>
    request(`/api/pickups/${pickupId}/assign`, {
      method: 'PUT',
      body: JSON.stringify({ recyclerCenterId }),
    }),
  schedulePickup: (pickupId, pickupDate, pickupTime) =>
    request(`/api/pickups/${pickupId}/schedule`, {
      method: 'PUT',
      body: JSON.stringify({ pickupDate, pickupTime }),
    }),
  updatePickupStatus: (pickupId, status) =>
    request(`/api/pickups/${pickupId}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    }),

  getImpact: () => request('/api/impact'),

  // Demo-mode support - see docs/OOP_CONCEPTS.md / README "Limitations" for
  // why this is not a real authentication system.
  getUsers: () => request('/api/users'),
  getRecyclerCenters: () => request('/api/recycler-centers'),
}
