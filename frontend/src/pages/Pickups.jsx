import { useEffect, useState } from 'react'
import { api } from '../services/api.js'
import StatusBadge from '../components/StatusBadge.jsx'
import PickupTimeline from '../components/PickupTimeline.jsx'
import EmptyState from '../components/EmptyState.jsx'

const PRIORITY_LABELS = { 1: 'HIGH', 2: 'MEDIUM', 3: 'NORMAL', 4: 'LOW' }
// Which single step each status can advance to WITHOUT extra data - used
// only for the generic "Mark as X" button. ASSIGNED and SCHEDULED are
// deliberately not here: assigning needs a recycler center and scheduling
// needs a date/time, so those two transitions have their own controls
// below rather than this generic button.
const SIMPLE_NEXT_STATUS = {
  SCHEDULED: 'COLLECTED',
  COLLECTED: 'RECYCLED',
}

export default function Pickups({ currentUser, onNavigate }) {
  const [pickups, setPickups] = useState([])
  const [recyclerCenters, setRecyclerCenters] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [recyclerChoice, setRecyclerChoice] = useState({})
  const [scheduleInputs, setScheduleInputs] = useState({})

  const isRecycler = currentUser?.role === 'RECYCLER'

  function loadPickups() {
    if (!currentUser) return
    setLoading(true)
    // Citizens see only their own requests; the recycler operator view
    // needs to see everyone's pending/active pickups to manage them.
    const fetchPickups = isRecycler ? api.getPickups() : api.getPickups(currentUser.id)
    Promise.all([fetchPickups, api.getRecyclerCenters()])
      .then(([pickupsData, centers]) => {
        setPickups(pickupsData)
        setRecyclerCenters(centers)
      })
      .catch(() => setError('Unable to load pickups. Please try again.'))
      .finally(() => setLoading(false))
  }

  useEffect(loadPickups, [currentUser])

  async function handleAssign(pickup) {
    const recyclerCenterId = recyclerChoice[pickup.id]
    if (!recyclerCenterId) return
    setActionError('')
    try {
      await api.assignRecycler(pickup.id, Number(recyclerCenterId))
      loadPickups()
    } catch (err) {
      setActionError(err.message)
    }
  }

  async function handleSchedule(pickup) {
    const input = scheduleInputs[pickup.id]
    if (!input?.date || !input?.time) return
    setActionError('')
    try {
      await api.schedulePickup(pickup.id, input.date, input.time)
      loadPickups()
    } catch (err) {
      setActionError(err.message)
    }
  }

  async function handleAdvanceStatus(pickup) {
    const next = SIMPLE_NEXT_STATUS[pickup.status]
    if (!next) return
    setActionError('')
    try {
      await api.updatePickupStatus(pickup.id, next)
      loadPickups()
    } catch (err) {
      setActionError(err.message)
    }
  }

  if (!currentUser || loading) return <p className="muted">Loading pickups...</p>
  if (error) return <p className="error-text">{error}</p>

  return (
    <div className="panel">
      <h2>{isRecycler ? 'Pickup Requests' : 'My Pickups'}</h2>
      <p className="panel-subtitle muted">
        {isRecycler
          ? 'Manage every pickup request as the recycler operator.'
          : 'Track the status of your own pickup requests.'}
      </p>
      {actionError && <p className="error-text">{actionError}</p>}

      {pickups.length === 0 ? (
        <EmptyState
          title="No pickup requests yet."
          message={isRecycler ? undefined : 'Request a pickup from the My Items page.'}
          actionLabel={isRecycler ? undefined : 'View My Items'}
          onAction={isRecycler ? undefined : () => onNavigate && onNavigate('myitems')}
        />
      ) : (
        <div className="pickup-list">
          {pickups.map((pickup) => (
            <div key={pickup.id} className="card pickup-card">
              <div className="pickup-card-header">
                <div>
                  <strong>Pickup #{pickup.id}</strong> — {pickup.item.itemType} ({pickup.pickupLocation})
                </div>
                <div className="pickup-card-tags">
                  <span className="badge badge-priority">{PRIORITY_LABELS[pickup.priorityWeight]} priority</span>
                  <StatusBadge status={pickup.status} />
                </div>
              </div>

              <PickupTimeline status={pickup.status} />

              {pickup.recyclerCenter && (
                <p className="muted pickup-meta">Recycler: {pickup.recyclerCenter.name} ({pickup.recyclerCenter.location})</p>
              )}
              {pickup.pickupDate && pickup.pickupTime && (
                <p className="muted pickup-meta">Scheduled for {pickup.pickupDate} at {pickup.pickupTime}</p>
              )}

              {isRecycler && (
                <div className="pickup-operator-controls">
                  {pickup.status === 'PENDING' && (
                    <div className="operator-field">
                      <label>Assign Recycler Center</label>
                      <div className="operator-inline">
                        <select
                          value={recyclerChoice[pickup.id] || ''}
                          onChange={(e) => setRecyclerChoice((prev) => ({ ...prev, [pickup.id]: e.target.value }))}
                        >
                          <option value="">Select a center...</option>
                          {recyclerCenters.map((c) => (
                            <option key={c.id} value={c.id}>{c.name} ({c.location})</option>
                          ))}
                        </select>
                        <button className="btn btn-secondary" onClick={() => handleAssign(pickup)}>Assign</button>
                      </div>
                    </div>
                  )}

                  {pickup.status === 'ASSIGNED' && (
                    <div className="operator-field">
                      <label>Schedule Pickup</label>
                      <div className="operator-inline">
                        <input
                          type="date"
                          onChange={(e) => setScheduleInputs((prev) => ({
                            ...prev,
                            [pickup.id]: { ...prev[pickup.id], date: e.target.value },
                          }))}
                        />
                        <input
                          type="time"
                          onChange={(e) => setScheduleInputs((prev) => ({
                            ...prev,
                            [pickup.id]: { ...prev[pickup.id], time: e.target.value },
                          }))}
                        />
                        <button className="btn btn-secondary" onClick={() => handleSchedule(pickup)}>Schedule</button>
                      </div>
                    </div>
                  )}

                  {SIMPLE_NEXT_STATUS[pickup.status] && (
                    <button className="btn btn-primary" onClick={() => handleAdvanceStatus(pickup)}>
                      Mark as {SIMPLE_NEXT_STATUS[pickup.status]}
                    </button>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
