import { useEffect, useState } from 'react'
import { api } from '../services/api.js'
import StatCard from '../components/StatCard.jsx'
import EmptyState from '../components/EmptyState.jsx'
import HeroIllustration from '../components/HeroIllustration.jsx'

export default function Dashboard({ onNavigate }) {
  const [impact, setImpact] = useState(null)
  const [recent, setRecent] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([api.getImpact(), api.getRecentItems(5)])
      .then(([impactData, recentItems]) => {
        setImpact(impactData)
        setRecent(recentItems)
      })
      .catch(() => setError('Unable to load dashboard data. Please try again.'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="dashboard">
      <section className="hero-band">
        <div className="hero">
          <div className="hero-text">
            <span className="hero-eyebrow">E-waste collection platform</span>
            <h1>Give your old electronics a second life.</h1>
            <p className="hero-subtitle">
              Register unwanted devices, request a pickup, and track responsible
              recycling from request to completion.
            </p>
            <div className="hero-actions">
              <button className="btn btn-primary" onClick={() => onNavigate('register')}>
                Register E-Waste
              </button>
              <button className="btn btn-secondary" onClick={() => onNavigate('myitems')}>
                View My Items
              </button>
            </div>
          </div>
          <div className="hero-visual">
            <HeroIllustration />
          </div>
        </div>
      </section>

      {error && <p className="error-text">{error}</p>}

      <div className="dashboard-grid">
        <section className="panel dashboard-main">
          <h2>Recent Activity</h2>
          {loading ? (
            <p className="muted">Loading dashboard...</p>
          ) : recent.length === 0 ? (
            <EmptyState
              title="No e-waste registered yet."
              message="Register your first device to start tracking its recycling journey."
              actionLabel="Register E-Waste"
              onAction={() => onNavigate('register')}
            />
          ) : (
            <ul className="activity-list">
              {recent.map((item) => (
                <li key={item.id} className="activity-item">
                  <div>
                    <strong>{item.itemType}</strong> registered — {item.location}
                  </div>
                  <span className="muted">{item.estimatedReward.toFixed(0)} pts est.</span>
                </li>
              ))}
            </ul>
          )}
        </section>

        {impact && (
          <aside className="dashboard-aside">
            <StatCard label="Items Registered" value={impact.totalItemsRegistered} />
            <StatCard label="Active Pickups" value={impact.activePickups} />
            <StatCard label="Eco Points (est.)" value={impact.totalEcoPoints.toFixed(0)} />
            <StatCard label="E-Waste Diverted (est.)" value={`${impact.estimatedEwasteDivertedKg.toFixed(1)} kg`} />
          </aside>
        )}
      </div>
    </div>
  )
}
