import { useState, useEffect } from 'react'
import { waterQualityApi } from '../api'
import { useWebSocket } from '../hooks/useWebSocket'

const THRESHOLDS = {
  temperature: { min: 22, max: 28, warnMin: 20, warnMax: 30, label: '水温', unit: '℃' },
  ph: { min: 6.5, max: 8.5, warnMin: 6.0, warnMax: 9.0, label: 'pH值', unit: '' },
  chlorine: { min: 0, max: 0.01, warnMin: 0, warnMax: 0.05, label: '残余氯气', unit: 'mg/L' },
  dissolvedOxygen: { min: 5, max: 20, warnMin: 4, warnMax: 25, label: '溶氧量', unit: 'mg/L' },
}

function getStatus(key, value) {
  if (value == null) return 'normal'
  const t = THRESHOLDS[key]
  if (!t) return 'normal'
  if (value < t.min || value > t.max) return 'critical'
  if (value < t.warnMin || value > t.warnMax) return 'warning'
  return 'normal'
}

function getStatusText(status) {
  const map = { normal: '正常', warning: '偏离', critical: '异常' }
  return map[status] || status
}

export default function Dashboard() {
  const [tankId, setTankId] = useState('TANK-1')
  const [sensorData, setSensorData] = useState(null)
  const [loading, setLoading] = useState(true)
  const { data: wsData, connected } = useWebSocket('/ws/aquarium')

  useEffect(() => {
    loadLatestData()
  }, [tankId])

  useEffect(() => {
    if (wsData && wsData.type === 'SENSOR_DATA' && wsData.payload) {
      setSensorData(wsData.payload)
    }
  }, [wsData])

  const loadLatestData = async () => {
    try {
      setLoading(true)
      const data = await waterQualityApi.getLatest(tankId)
      setSensorData(data)
    } catch {
      setSensorData(null)
    } finally {
      setLoading(false)
    }
  }

  const metrics = sensorData
    ? [
        { key: 'temperature', value: sensorData.temperature },
        { key: 'ph', value: sensorData.ph },
        { key: 'chlorine', value: sensorData.chlorine },
        { key: 'dissolvedOxygen', value: sensorData.dissolvedOxygen },
      ]
    : []

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">
          <span className={connected ? 'live-dot' : ''} style={{ background: connected ? '#52c41a' : '#999' }} />
          实时监控
        </h1>
        <select className="tank-selector" value={tankId} onChange={e => setTankId(e.target.value)}>
          <option value="TANK-1">1号缸</option>
          <option value="TANK-2">2号缸</option>
          <option value="TANK-3">3号缸</option>
        </select>
      </div>

      {loading && !sensorData ? (
        <div className="loading">加载中...</div>
      ) : sensorData ? (
        <>
          <div className="metric-grid">
            {metrics.map(({ key, value }) => {
              const t = THRESHOLDS[key]
              const status = getStatus(key, value)
              return (
                <div key={key} className={`metric-card ${status}`}>
                  <div className="metric-label">{t.label}</div>
                  <div className="metric-value" style={{ color: status === 'critical' ? '#ff4d4f' : status === 'warning' ? '#faad14' : '#333' }}>
                    {value != null ? value.toFixed(2) : '--'}
                  </div>
                  <div className="metric-unit">{t.unit}</div>
                  <div className={`metric-status ${status}`}>{getStatusText(status)}</div>
                </div>
              )
            })}
          </div>

          <div className="card" style={{ marginTop: 12 }}>
            <div className="section-title">安全范围参考</div>
            {metrics.map(({ key }) => {
              const t = THRESHOLDS[key]
              return (
                <div key={key} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid #f0f0f0' }}>
                  <span style={{ fontSize: 13 }}>{t.label}</span>
                  <span style={{ fontSize: 13, color: '#999' }}>
                    {t.min} ~ {t.max} {t.unit}
                  </span>
                </div>
              )
            })}
          </div>
        </>
      ) : (
        <div className="empty-state">
          <div className="icon">🐟</div>
          <div>暂无传感器数据</div>
        </div>
      )}
    </div>
  )
}
