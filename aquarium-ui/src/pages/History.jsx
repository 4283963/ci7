import { useState, useEffect } from 'react'
import { historyApi } from '../api'

const METRIC_CONFIG = {
  temperature: { label: '水温', unit: '℃', color: '#ff6b6b', max: 35, min: 15 },
  ph: { label: 'pH值', unit: '', color: '#4ecdc4', max: 10, min: 4 },
  chlorine: { label: '残余氯气', unit: 'mg/L', color: '#45b7d1', max: 0.1, min: 0 },
  dissolvedOxygen: { label: '溶氧量', unit: 'mg/L', color: '#96ceb4', max: 25, min: 0 },
}

function MiniChart({ records, metricKey }) {
  const config = METRIC_CONFIG[metricKey]
  if (!config || !records || records.length === 0) return null

  const latestRecords = records.slice(0, 20).reverse()
  const range = config.max - config.min

  return (
    <div className="history-chart">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <span style={{ fontSize: 14, fontWeight: 600 }}>{config.label}</span>
        <span style={{ fontSize: 12, color: '#999' }}>{config.unit}</span>
      </div>
      <div className="chart-bar-container">
        {latestRecords.map((record, i) => {
          const value = record[metricKey]
          if (value == null) return <div key={i} style={{ flex: 1 }} />

          const pct = Math.max(0, Math.min(100, ((value - config.min) / range) * 100))
          const height = Math.max(4, pct * 1.1)

          return (
            <div
              key={i}
              className="chart-bar"
              style={{ height: `${height}%`, background: config.color, opacity: 0.6 + (pct / 250) }}
              data-value={`${value.toFixed(2)}${config.unit}`}
            />
          )
        })}
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6 }}>
        <span style={{ fontSize: 10, color: '#999' }}>
          {latestRecords[0]?.recordedAt
            ? new Date(latestRecords[0].recordedAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
            : ''}
        </span>
        <span style={{ fontSize: 10, color: '#999' }}>
          {latestRecords[latestRecords.length - 1]?.recordedAt
            ? new Date(latestRecords[latestRecords.length - 1].recordedAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
            : ''}
        </span>
      </div>
    </div>
  )
}

export default function History() {
  const [tankId, setTankId] = useState('TANK-1')
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadHistory()
  }, [tankId])

  const loadHistory = async () => {
    try {
      setLoading(true)
      const data = await historyApi.getLatest(tankId, 30)
      setRecords(data || [])
    } catch {
      setRecords([])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">历史数据</h1>
        <select className="tank-selector" value={tankId} onChange={e => setTankId(e.target.value)}>
          <option value="TANK-1">1号缸</option>
          <option value="TANK-2">2号缸</option>
          <option value="TANK-3">3号缸</option>
        </select>
      </div>

      {loading ? (
        <div className="loading">加载中...</div>
      ) : records.length > 0 ? (
        Object.keys(METRIC_CONFIG).map(key => (
          <MiniChart key={key} records={records} metricKey={key} />
        ))
      ) : (
        <div className="empty-state">
          <div className="icon">📈</div>
          <div>暂无历史数据</div>
        </div>
      )}
    </div>
  )
}
