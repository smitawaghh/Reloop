import { useState } from 'react'
import { api } from '../services/api.js'

const ITEM_TYPES = [
  { value: 'LAPTOP', label: 'Laptop' },
  { value: 'MOBILE_PHONE', label: 'Mobile Phone' },
  { value: 'BATTERY', label: 'Battery' },
  { value: 'ACCESSORY', label: 'Accessory' },
]

const CONDITIONS = [
  { value: 'WORKING', label: 'Working' },
  { value: 'PARTIALLY_WORKING', label: 'Partially Working' },
  { value: 'DAMAGED', label: 'Damaged' },
]

const initialForm = {
  itemType: 'LAPTOP',
  condition: 'WORKING',
  weightKg: '',
  location: '',
  description: '',
}

export default function RegisterEwaste({ onNavigate, currentUser }) {
  const [form, setForm] = useState(initialForm)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState(null)

  function updateField(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const saved = await api.registerItem({
        ...form,
        weightKg: Number(form.weightKg),
        userId: currentUser?.id,
      })
      setResult(saved)
      setForm(initialForm)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (result) {
    return (
      <div className="panel success-panel">
        <h2>Item Registered</h2>
        <p>Your device has been registered successfully.</p>
        <div className="success-details">
          <div><span>Item ID</span><strong>#{result.id}</strong></div>
          <div><span>Device Type</span><strong>{result.itemType}</strong></div>
          <div><span>Estimated Eco Points</span><strong>{result.estimatedReward.toFixed(0)}</strong></div>
        </div>
        <div className="form-actions">
          <button className="btn btn-primary" onClick={() => onNavigate('myitems')}>View My Items</button>
          <button className="btn btn-secondary" onClick={() => setResult(null)}>Register Another</button>
        </div>
      </div>
    )
  }

  return (
    <div className="panel">
      <h2>Register E-Waste</h2>
      <p className="muted">Tell us about your device and we'll take care of the rest.</p>
      {currentUser && <p className="muted">Registering as <strong>{currentUser.name}</strong>.</p>}
      <form className="form" onSubmit={handleSubmit}>
        <div className="form-row">
          <label>
            Device Type
            <select value={form.itemType} onChange={(e) => updateField('itemType', e.target.value)}>
              {ITEM_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
            </select>
          </label>
          <label>
            Condition
            <select value={form.condition} onChange={(e) => updateField('condition', e.target.value)}>
              {CONDITIONS.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
            </select>
          </label>
        </div>
        <div className="form-row">
          <label>
            Weight (kg)
            <input
              type="number"
              step="0.1"
              min="0.1"
              required
              value={form.weightKg}
              onChange={(e) => updateField('weightKg', e.target.value)}
            />
          </label>
          <label>
            Pickup Location
            <input
              type="text"
              required
              placeholder="e.g. College Hostel"
              value={form.location}
              onChange={(e) => updateField('location', e.target.value)}
            />
          </label>
        </div>
        <label>
          Description (optional)
          <input
            type="text"
            placeholder="e.g. Dell Inspiron, 2020 model"
            value={form.description}
            onChange={(e) => updateField('description', e.target.value)}
          />
        </label>

        {error && <p className="error-text">{error}</p>}

        <button className="btn btn-primary" type="submit" disabled={submitting}>
          {submitting ? 'Submitting...' : 'Submit'}
        </button>
      </form>
    </div>
  )
}
