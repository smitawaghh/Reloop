const STEPS = ['PENDING', 'ASSIGNED', 'SCHEDULED', 'COLLECTED', 'RECYCLED']
const STEP_LABELS = {
  PENDING: 'Registered',
  ASSIGNED: 'Recycler Assigned',
  SCHEDULED: 'Pickup Scheduled',
  COLLECTED: 'Collected',
  RECYCLED: 'Recycled',
}

export default function PickupTimeline({ status }) {
  const currentIndex = STEPS.indexOf(status)

  return (
    <div className="timeline">
      {STEPS.map((step, index) => (
        <div
          key={step}
          className={`timeline-step ${index <= currentIndex ? 'done' : ''} ${index === currentIndex ? 'current' : ''}`}
        >
          <div className="timeline-dot" />
          <div className="timeline-label">{STEP_LABELS[step]}</div>
        </div>
      ))}
    </div>
  )
}
