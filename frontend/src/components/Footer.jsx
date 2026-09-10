import Logo from './Logo.jsx'

export default function Footer({ onNavigate }) {
  return (
    <footer className="footer">
      <div className="footer-inner">
        <div>
          <div className="footer-brand"><Logo size={20} /><span>ReLoop</span></div>
          <p className="footer-tagline">Give your old electronics a second life.</p>
        </div>

        <div>
          <p className="footer-heading">Platform</p>
          <ul className="footer-links">
            <li><button onClick={() => onNavigate('dashboard')}>Dashboard</button></li>
            <li><button onClick={() => onNavigate('myitems')}>My Items</button></li>
            <li><button onClick={() => onNavigate('pickups')}>Pickups</button></li>
            <li><button onClick={() => onNavigate('impact')}>Impact</button></li>
          </ul>
        </div>

        <div>
          <p className="footer-heading">About</p>
          <ul className="footer-links">
            <li><span>Responsible e-waste collection</span></li>
            <li><span>Circular economy</span></li>
            <li><span>Community impact</span></li>
          </ul>
        </div>
      </div>
      <div className="footer-bottom">
        © {new Date().getFullYear()} ReLoop. A student portfolio project.
      </div>
    </footer>
  )
}
