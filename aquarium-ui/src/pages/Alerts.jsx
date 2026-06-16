import { useState, useEffect } from 'react'
import { alertApi } from '../api'

const METRIC_LABELS = {
  temperature: '水温',
  ph: 'pH值',
  chlorine: '残余氯气',
  dissolved_oxygen: '溶氧量',
}

const LEVEL_LABELS = {
  info: '提示',
  warning: '警告',
  critical: '严重',
  fatal: '致命',
}

export default function Alerts() {
  const [tankId, setTankId] = useState('TANK-1')
  const [alerts, setAlerts] = useState([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('all')

  useEffect(() => {
    loadAlerts()
  }, [tankId])

  const loadAlerts = async () => {
    try {
      setLoading(true)
      const data = await alertApi.getByTank(tankId)
      setAlerts(data || [])
    } catch {
      setAlerts([])
    } finally {
      setLoading(false)
    }
  }

  const filteredAlerts = filter === 'all'
    ? alerts
    : alerts.filter(a => a.level === filter)

  const sortedAlerts = [...filteredAlerts].sort((a, b) => {
    const levelOrder = { fatal: 0, critical: 1, warning: 2, info: 3 }
    return (levelOrder[a.level] ?? 99) - (levelOrder[b.level] ?? 99)
  })

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">告警中心</h1>
        <select className="tank-selector" value={tankId} onChange={e => setTankId(e.target.value)}>
          <option value="TANK-1">1号缸</option>
          <option value="TANK-2">2号缸</option>
          <option value="TANK-3">3号缸</option>
        </select>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
        {['all', 'critical', 'warning', 'info'].map(level => (
          <button
            key={level}
            className={`action-btn ${filter === level ? 'active' : ''}`}
            style={filter === level ? { background: '#1890ff', color: 'white', borderColor: '#1890ff' } : {}}
            onClick={() => setFilter(level)}
          >
            {level === 'all' ? '全部' : LEVEL_LABELS[level] || level}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="loading">加载中...</div>
      ) : sortedAlerts.length > 0 ? (
        sortedAlerts.map(alert => (
          <div key={alert.alertId || alert.id} className={`alert-item ${alert.level}`}>
            <div className="alert-header">
              <span className="alert-metric">{METRIC_LABELS[alert.metric] || alert.metric}</span>
              <span className={`alert-level ${alert.level}`}>
                {LEVEL_LABELS[alert.level] || alert.level}
              </span>
            </div>
            <div className="alert-message">{alert.message}</div>
            <div className="alert-time">
              {alert.createdAt
                ? new Date(alert.createdAt).toLocaleString('zh-CN')
                : ''}
            </div>
          </div>
        ))
      ) : (
        <div className="empty-state">
          <div className="icon">✅</div>
          <div>暂无告警记录</div>
        </div>
      )}
    </div>
  )
}
