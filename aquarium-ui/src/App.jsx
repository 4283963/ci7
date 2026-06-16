import { Routes, Route, Navigate } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import DeviceControl from './pages/DeviceControl'
import Breeding from './pages/Breeding'
import Alerts from './pages/Alerts'
import History from './pages/History'
import BottomNav from './components/BottomNav'

function App() {
  return (
    <div className="app-container">
      <div className="page-content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/devices" element={<DeviceControl />} />
          <Route path="/breeding" element={<Breeding />} />
          <Route path="/alerts" element={<Alerts />} />
          <Route path="/history" element={<History />} />
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </div>
      <BottomNav />
    </div>
  )
}

export default App
