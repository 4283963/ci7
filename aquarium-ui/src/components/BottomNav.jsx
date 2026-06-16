import { NavLink } from 'react-router-dom'

const navItems = [
  { path: '/', label: '监控', icon: '📊' },
  { path: '/devices', label: '设备', icon: '🎛️' },
  { path: '/alerts', label: '告警', icon: '🔔' },
  { path: '/history', label: '历史', icon: '📈' },
]

export default function BottomNav() {
  return (
    <nav className="bottom-nav">
      {navItems.map(item => (
        <NavLink
          key={item.path}
          to={item.path}
          className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
        >
          <span className="nav-icon">{item.icon}</span>
          <span className="nav-label">{item.label}</span>
        </NavLink>
      ))}
    </nav>
  )
}
