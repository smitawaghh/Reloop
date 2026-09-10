import { useEffect, useState } from 'react'
import { api } from '../services/api.js'
import EwasteCard from '../components/EwasteCard.jsx'
import EmptyState from '../components/EmptyState.jsx'

// An item's pickup status isn't a field on the item itself (that would
// duplicate state that Pickup already owns) - so we derive it here by
// matching each item to its most recent pickup, defaulting to
// NOT_REQUESTED when none exists yet.
function buildStatusByItemId(pickups) {
  const map = {}
  for (const pickup of pickups) {
    map[pickup.item.id] = pickup.status
  }
  return map
}

export default function MyItems({ currentUser, onNavigate }) {
  const [items, setItems] = useState([])
  const [statusByItemId, setStatusByItemId] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')

  function loadData() {
    if (!currentUser) return
    setLoading(true)
    Promise.all([api.getItems(currentUser.id), api.getPickups(currentUser.id)])
      .then(([itemsData, pickupsData]) => {
        setItems(itemsData)
        setStatusByItemId(buildStatusByItemId(pickupsData))
      })
      .catch(() => setError('Unable to load your items. Please try again.'))
      .finally(() => setLoading(false))
  }

  useEffect(loadData, [currentUser])

  async function handleRequestPickup(item) {
    setActionError('')
    try {
      await api.requestPickup(item.id, item.location, currentUser?.id)
      loadData()
    } catch (err) {
      setActionError(err.message)
    }
  }

  if (!currentUser || loading) return <p className="muted">Loading your items...</p>
  if (error) return <p className="error-text">{error}</p>

  return (
    <div className="panel">
      <h2>My Items</h2>
      <p className="panel-subtitle muted">Devices registered by <strong>{currentUser.name}</strong>.</p>
      {actionError && <p className="error-text">{actionError}</p>}

      {items.length === 0 ? (
        <EmptyState
          title="No e-waste registered yet."
          message="Register a device to see it show up here."
          actionLabel="Register E-Waste"
          onAction={() => onNavigate && onNavigate('register')}
        />
      ) : (
        <div className="card-grid">
          {items.map((item) => (
            <EwasteCard
              key={item.id}
              item={item}
              status={statusByItemId[item.id] || 'NOT_REQUESTED'}
              onRequestPickup={handleRequestPickup}
            />
          ))}
        </div>
      )}
    </div>
  )
}
