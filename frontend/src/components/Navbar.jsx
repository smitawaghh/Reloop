import { useState } from 'react'
import Logo from './Logo.jsx'

// No react-router here on purpose: five pages and one nav bar don't need a
// routing library - `page` state in App.jsx and a click handler are enough,
// and it keeps the dependency list at exactly React + Vite.
function navItemsForRole(role) {
  if (role === 'RECYCLER') {
    return [
      { id: 'dashboard', label: 'Dashboard' },
      { id: 'pickups', label: 'Pickup Requests' },
      { id: 'impact', label: 'Impact' },
    ]
  }
  return [
    { id: 'dashboard', label: 'Dashboard' },
    { id: 'myitems', label: 'My Items' },
    { id: 'pickups', label: 'My Pickups' },
    { id: 'impact', label: 'Impact' },
  ]
}

export default function Navbar({ page, onNavigate, users, currentUser, onSwitchUser }) {
  const [menuOpen, setMenuOpen] = useState(false)
  const isRecycler = currentUser?.role === 'RECYCLER'
  const navItems = navItemsForRole(currentUser?.role)

  return (
    <header className="navbar">
      <div className="navbar-row">
        <div className="navbar-brand" onClick={() => onNavigate('dashboard')}>
          <Logo />
          <span>ReLoop</span>
        </div>

        <button className="navbar-toggle" onClick={() => setMenuOpen((v) => !v)} aria-label="Toggle menu">
          <span />
          <span />
          <span />
        </button>

        <div className={`navbar-collapsible ${menuOpen ? 'open' : ''}`}>
          <nav className="navbar-links">
            {navItems.map((item) => (
              <button
                key={item.id}
                className={`navbar-link ${page === item.id ? 'active' : ''}`}
                onClick={() => { onNavigate(item.id); setMenuOpen(false) }}
              >
                {item.label}
              </button>
            ))}
          </nav>

          <div className="navbar-right">
            {/* Acting-as switcher: this is NOT a login. There's no password,
                no session, no server-side identity check - it just tells the
                backend which demo user's id to attach to new items/pickups.
                See README "Limitations". */}
            {users.length > 0 && (
              <label className="user-switcher" title="Prototype account switcher - not a real login">
                <span className="user-switcher-caption">Acting as</span>
                <select
                  value={currentUser?.id || ''}
                  onChange={(e) => onSwitchUser(users.find((u) => u.id === Number(e.target.value)))}
                >
                  {users.map((u) => (
                    <option key={u.id} value={u.id}>{u.name} · {u.role === 'RECYCLER' ? 'Recycler' : 'Citizen'}</option>
                  ))}
                </select>
              </label>
            )}

            {!isRecycler && (
              <button className="btn btn-primary navbar-cta" onClick={() => { onNavigate('register'); setMenuOpen(false) }}>
                Register E-Waste
              </button>
            )}
          </div>
        </div>
      </div>
    </header>
  )
}
