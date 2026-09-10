import StatusBadge from './StatusBadge.jsx'

const INITIALS = {
  Laptop: 'LP',
  'Mobile Phone': 'MP',
  Battery: 'BT',
  Accessory: 'AC',
}

export default function EwasteCard({ item, status, onRequestPickup }) {
  return (
    <div className="card ewaste-card">
      <div className="ewaste-card-header">
        <span className="monogram">{INITIALS[item.itemType] || '?'}</span>
        <div>
          <div className="ewaste-type">{item.itemType}</div>
          <div className="ewaste-description">{item.description || 'No description'}</div>
        </div>
      </div>
      <div className="ewaste-details">
        <div><span>Weight</span><strong>{item.weightKg} kg</strong></div>
        <div><span>Condition</span><strong>{item.condition.replace('_', ' ')}</strong></div>
        <div><span>Location</span><strong>{item.location}</strong></div>
        <div><span>Eco Points</span><strong>{item.estimatedReward.toFixed(0)}</strong></div>
      </div>
      <div className="ewaste-card-footer">
        <StatusBadge status={status} />
        {status === 'NOT_REQUESTED' && onRequestPickup && (
          <button className="btn btn-secondary" onClick={() => onRequestPickup(item)}>
            Request Pickup
          </button>
        )}
      </div>
    </div>
  )
}
