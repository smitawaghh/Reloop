const STATUS_CLASSES = {
  PENDING: 'badge-pending',
  ASSIGNED: 'badge-assigned',
  SCHEDULED: 'badge-scheduled',
  COLLECTED: 'badge-collected',
  RECYCLED: 'badge-recycled',
  NOT_REQUESTED: 'badge-not-requested',
}

const LABELS = {
  NOT_REQUESTED: 'No Pickup Yet',
}

export default function StatusBadge({ status }) {
  const className = STATUS_CLASSES[status] || 'badge-pending'
  return <span className={`badge ${className}`}>{LABELS[status] || status}</span>
}
