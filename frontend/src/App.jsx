import { useEffect, useState } from 'react'
import { api } from './services/api.js'
import Navbar from './components/Navbar.jsx'
import Footer from './components/Footer.jsx'
import Dashboard from './pages/Dashboard.jsx'
import RegisterEwaste from './pages/RegisterEwaste.jsx'
import MyItems from './pages/MyItems.jsx'
import Pickups from './pages/Pickups.jsx'
import Impact from './pages/Impact.jsx'

const CURRENT_USER_ID_KEY = 'reloop.currentUserId'

// A five-page app doesn't need react-router: one piece of state for "which
// page is active" plus a switch is simpler to read and explain, and it's
// one dependency fewer.
export default function App() {
  const [page, setPage] = useState('dashboard')
  const [users, setUsers] = useState([])
  const [currentUser, setCurrentUser] = useState(null)

  // There's no login in this prototype (see README "Limitations"). Instead,
  // the app loads the seeded demo users and lets you switch which one
  // you're "acting as" - the choice is remembered in localStorage only for
  // convenience across reloads, it isn't a session or credential of any
  // kind.
  useEffect(() => {
    api.getUsers().then((fetchedUsers) => {
      setUsers(fetchedUsers)
      const savedId = Number(localStorage.getItem(CURRENT_USER_ID_KEY))
      const restored = fetchedUsers.find((u) => u.id === savedId)
      setCurrentUser(restored || fetchedUsers[0] || null)
    })
  }, [])

  function handleSwitchUser(user) {
    setCurrentUser(user)
    localStorage.setItem(CURRENT_USER_ID_KEY, String(user.id))
  }

  return (
    <div className="app">
      <Navbar page={page} onNavigate={setPage} users={users} currentUser={currentUser} onSwitchUser={handleSwitchUser} />
      <main className="page-container">
        {page === 'dashboard' && <Dashboard onNavigate={setPage} />}
        {page === 'register' && <RegisterEwaste onNavigate={setPage} currentUser={currentUser} />}
        {page === 'myitems' && <MyItems currentUser={currentUser} onNavigate={setPage} />}
        {page === 'pickups' && <Pickups currentUser={currentUser} onNavigate={setPage} />}
        {page === 'impact' && <Impact />}
      </main>
      <Footer onNavigate={setPage} />
    </div>
  )
}
