import { useState, useEffect } from 'react'
import { deviceApi, iotApi } from '../api'

const DEVICE_LABELS = {
  heater: { name: '加温棒', icon: '🔥' },
  chlorine_valve: { name: '排氯补水阀', icon: '💧' },
  water_pump: { name: '水泵', icon: '🔄' },
  aeration_pump: { name: '增氧泵', icon: '💨' },
}

const DEVICE_ACTIONS = {
  heater: [
    { action: 'on', label: '开启加热', params: {} },
    { action: 'off', label: '关闭加热', params: {} },
    { action: 'set_temp', label: '设定25℃', params: { target_temp: 25 } },
  ],
  chlorine_valve: [
    { action: 'open', label: '排氯开启', params: { duration_seconds: 60 } },
    { action: 'close', label: '关闭阀门', params: {} },
    { action: 'refill', label: '补水30s', params: { duration_seconds: 30 } },
  ],
  water_pump: [
    { action: 'on', label: '开启水泵', params: { action: 'circulate', duration_seconds: 60 } },
    { action: 'off', label: '关闭水泵', params: {} },
  ],
  aeration_pump: [
    { action: 'on', label: '开启增氧', params: { duration_seconds: 120 } },
    { action: 'off', label: '关闭增氧', params: {} },
  ],
}

export default function DeviceControl() {
  const [tankId, setTankId] = useState('TANK-1')
  const [devices, setDevices] = useState([])
  const [loading, setLoading] = useState(true)
  const [operating, setOperating] = useState({})

  useEffect(() => {
    loadDevices()
  }, [tankId])

  const loadDevices = async () => {
    try {
      setLoading(true)
      const data = await deviceApi.getByTank(tankId)
      setDevices(data || [])
    } catch {
      setDevices([])
    } finally {
      setLoading(false)
    }
  }

  const handleToggle = async (device) => {
    const newAction = device.status === 'on' ? 'off' : 'on'
    const opKey = device.deviceId
    setOperating(prev => ({ ...prev, [opKey]: true }))

    try {
      await deviceApi.toggleDevice(device.deviceId, newAction)
      setDevices(prev =>
        prev.map(d =>
          d.deviceId === device.deviceId ? { ...d, status: newAction } : d
        )
      )
    } catch (err) {
      console.error('Toggle failed:', err)
    } finally {
      setOperating(prev => ({ ...prev, [opKey]: false }))
    }
  }

  const handleAction = async (deviceType, actionConfig) => {
    const opKey = `${deviceType}-${actionConfig.action}`
    setOperating(prev => ({ ...prev, [opKey]: true }))

    try {
      await iotApi.sendCommand({
        tankId,
        deviceType,
        action: actionConfig.action,
        params: actionConfig.params,
      })
    } catch (err) {
      console.error('Action failed:', err)
    } finally {
      setOperating(prev => ({ ...prev, [opKey]: false }))
    }
  }

  const defaultDevices = Object.entries(DEVICE_LABELS).map(([type, info]) => ({
    deviceId: `${tankId}-${type}`,
    deviceType: type,
    status: 'off',
    tankId,
    ...info,
  }))

  const displayDevices = devices.length > 0
    ? devices.map(d => ({
        ...d,
        ...DEVICE_LABELS[d.deviceType],
      }))
    : defaultDevices

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">设备控制</h1>
        <select className="tank-selector" value={tankId} onChange={e => setTankId(e.target.value)}>
          <option value="TANK-1">1号缸</option>
          <option value="TANK-2">2号缸</option>
          <option value="TANK-3">3号缸</option>
        </select>
      </div>

      <div className="device-list">
        {displayDevices.map(device => {
          const actions = DEVICE_ACTIONS[device.deviceType] || []
          const isOn = device.status === 'on'
          const opKey = device.deviceId

          return (
            <div key={device.deviceId} className="device-item" style={{ flexDirection: 'column', alignItems: 'stretch' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                <div className="device-info" style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span style={{ fontSize: 28 }}>{device.icon}</span>
                  <div>
                    <div className="device-name">{device.name}</div>
                    <div className={`device-status ${device.status}`}>
                      {device.status === 'on' ? '运行中' : device.status === 'off' ? '已关闭' : device.status === 'error' ? '故障' : '离线'}
                    </div>
                  </div>
                </div>
                <button
                  className={`toggle-btn ${isOn ? 'on' : 'off'}`}
                  onClick={() => handleToggle(device)}
                  disabled={operating[opKey]}
                />
              </div>

              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                {actions.map(ac => {
                  const btnKey = `${device.deviceType}-${ac.action}`
                  return (
                    <button
                      key={ac.action}
                      className="action-btn"
                      onClick={() => handleAction(device.deviceType, ac)}
                      disabled={operating[btnKey]}
                    >
                      {operating[btnKey] ? '执行中...' : ac.label}
                    </button>
                  )
                })}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
