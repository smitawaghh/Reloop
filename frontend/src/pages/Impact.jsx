import { useEffect, useState } from 'react'
import { api } from '../services/api.js'
import StatCard from '../components/StatCard.jsx'

export default function Impact() {
  const [impact, setImpact] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api.getImpact()
      .then(setImpact)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p className="muted">Loading impact data...</p>
  if (error) return <p className="error-text">Could not load impact data: {error}</p>

  const weightByType = impact.weightByTypeKg || {}
  const maxWeight = Math.max(1, ...Object.values(weightByType))

  return (
    <div className="panel">
      <h2>Environmental Impact</h2>
      <p className="muted">
        Together we're creating a cleaner, greener planet. <strong>These figures are estimates</strong>{' '}
        based on registered device weight and reward data, not measured environmental science.
      </p>

      <section className="stat-grid">
        <StatCard label="E-Waste Diverted (est.)" value={`${impact.estimatedEwasteDivertedKg.toFixed(1)} kg`} />
        <StatCard label="Devices Registered" value={impact.totalItemsRegistered} />
        <StatCard label="Eco Points Earned (est.)" value={impact.totalEcoPoints.toFixed(0)} />
        <StatCard label="Devices Recycled" value={impact.recycledCount} />
      </section>

      <section className="panel-inset">
        <h3>Weight Diverted by Device Type (est.)</h3>
        {Object.keys(weightByType).length === 0 ? (
          <p className="muted">No items registered yet.</p>
        ) : (
          <div className="bar-chart">
            {Object.entries(weightByType).map(([type, weight]) => (
              <div className="bar-row" key={type}>
                <span className="bar-label">{type}</span>
                <div className="bar-track">
                  <div className="bar-fill" style={{ width: `${(weight / maxWeight) * 100}%` }} />
                </div>
                <span className="bar-value">{weight.toFixed(1)} kg</span>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
