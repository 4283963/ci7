import { useState, useEffect, useCallback } from 'react'
import { breedingApi } from '../api'
import { useWebSocket } from '../hooks/useWebSocket'

const TANKS = [
  { id: 'TANK-1', name: '1号缸' },
  { id: 'TANK-2', name: '2号缸' },
  { id: 'TANK-3', name: '3号缸' },
]

const SPECIES = [
  { code: 'guppy', name: '孔雀鱼', icon: '🐟' },
  { code: 'molly', name: '玛丽鱼', icon: '🐠' },
  { code: 'platy', name: '月光鱼', icon: '🐡' },
  { code: 'betta', name: '斗鱼', icon: '🎏' },
]

function formatCountdown(seconds) {
  if (seconds < 0) return '已到期'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const mins = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  if (days > 0) return `${days}天${hours}时${mins}分`
  if (hours > 0) return `${hours}时${mins}分${secs}秒`
  return `${mins}分${secs}秒`
}

function BreedingCard({ tank, countdown, onStop, onMarkBirthed }) {
  const isMotherDue = countdown.motherRemovalDue
  const modeText = {
    preparing: '待产中',
    breeding: '繁殖中',
    hatching: '鱼苗期',
    idle: '空闲',
  }

  return (
    <div className="breeding-card active">
      <div className="breeding-card-header">
        <div>
          <span className="breeding-tank-name">{tank.name}</span>
          <span className="breeding-species">{countdown.speciesName}</span>
        </div>
        <span className={`breeding-status ${countdown.modeStatus}`}>
          {modeText[countdown.modeStatus] || countdown.modeStatus}
        </span>
      </div>

      {countdown.birthCountdownSeconds !== undefined && countdown.modeStatus === 'preparing' && (
        <div className="countdown-section">
          <div className="countdown-label">⏰ 预计产仔倒计时</div>
          <div className="countdown-value">{formatCountdown(countdown.birthCountdownSeconds)}</div>
        </div>
      )}

      {countdown.motherRemovalCountdownSeconds !== undefined && (
        <div className={`countdown-section ${isMotherDue ? 'due' : ''}`}>
          <div className="countdown-label">
            {isMotherDue ? '⚠️ 母鱼捞取时间到！' : '🐟 母鱼捞取倒计时'}
          </div>
          <div className="countdown-value">{formatCountdown(countdown.motherRemovalCountdownSeconds)}</div>
        </div>
      )}

      {countdown.frySafeCountdownSeconds !== undefined && (
        <div className="countdown-section subtle">
          <div className="countdown-label">🐣 鱼苗安全期</div>
          <div className="countdown-value small">{formatCountdown(countdown.frySafeCountdownSeconds)}</div>
        </div>
      )}

      <div className="breeding-params">
        <span>🌡️ {countdown.targetTemp}℃</span>
        <span>💧 pH {countdown.targetPh}</span>
        <span>🚫 过滤泵已关</span>
      </div>

      <div className="breeding-actions">
        {countdown.modeStatus === 'preparing' && (
          <button className="breeding-btn primary" onClick={() => onMarkBirthed(tank.id)}>
            已产仔 ✓
          </button>
        )}
        <button className="breeding-btn danger" onClick={() => onStop(tank.id)}>
          结束繁育
        </button>
      </div>
    </div>
  )
}

export default function Breeding() {
  const [selectedTanks, setSelectedTanks] = useState([])
  const [selectedSpecies, setSelectedSpecies] = useState('guppy')
  const [configs, setConfigs] = useState([])
  const [countdowns, setCountdowns] = useState({})
  const [loading, setLoading] = useState(true)
  const [operating, setOperating] = useState(false)
  const [now, setNow] = useState(Date.now())
  const { data: wsData } = useWebSocket('/ws/aquarium')

  useEffect(() => {
    loadData()
  }, [])

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [])

  useEffect(() => {
    if (wsData && wsData.type === 'BREEDING_UPDATE') {
      loadData()
    }
  }, [wsData])

  const loadData = async () => {
    try {
      setLoading(true)
      const [configList, cdMap] = await Promise.all([
        breedingApi.getAllConfigs(),
        breedingApi.getAllCountdowns().catch(() => ({})),
      ])
      setConfigs(configList || [])
      setCountdowns(cdMap || {})
    } catch (e) {
      console.error('Load breeding data failed:', e)
    } finally {
      setLoading(false)
    }
  }

  const activeTanks = TANKS.filter(t => countdowns[t.id]?.active)
  const idleTanks = TANKS.filter(t => !countdowns[t.id]?.active)

  const toggleTank = (tankId) => {
    setSelectedTanks(prev =>
      prev.includes(tankId)
        ? prev.filter(id => id !== tankId)
        : [...prev, tankId]
    )
  }

  const handleBatchStart = async () => {
    if (selectedTanks.length === 0) return
    setOperating(true)
    try {
      await breedingApi.batchStart(selectedTanks, selectedSpecies)
      setSelectedTanks([])
      loadData()
    } catch (e) {
      alert('启动失败: ' + e.message)
    } finally {
      setOperating(false)
    }
  }

  const handleStop = async (tankId) => {
    if (!confirm('确定要结束繁育模式吗？设备会恢复正常设置。')) return
    try {
      await breedingApi.stopBreeding(tankId)
      loadData()
    } catch (e) {
      alert('操作失败: ' + e.message)
    }
  }

  const handleMarkBirthed = async (tankId) => {
    if (!confirm('确认已产仔？将启动母鱼捞取倒计时。')) return
    try {
      await breedingApi.markAsBirthed(tankId)
      loadData()
    } catch (e) {
      alert('操作失败: ' + e.message)
    }
  }

  const selectAllIdle = () => {
    setSelectedTanks(idleTanks.map(t => t.id))
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">🐟 繁育模式</h1>
      </div>

      {activeTanks.length > 0 && (
        <>
          <div className="section-title">
            进行中 ({activeTanks.length})
          </div>
          {activeTanks.map(tank => (
            <BreedingCard
              key={tank.id}
              tank={tank}
              countdown={{
                ...countdowns[tank.id],
                birthCountdownSeconds: adjustCountdown(countdowns[tank.id]?.birthCountdownSeconds),
                motherRemovalCountdownSeconds: adjustCountdown(countdowns[tank.id]?.motherRemovalCountdownSeconds),
                frySafeCountdownSeconds: adjustCountdown(countdowns[tank.id]?.frySafeCountdownSeconds),
              }}
              onStop={handleStop}
              onMarkBirthed={handleMarkBirthed}
            />
          ))}
        </>
      )}

      <div className="card" style={{ marginTop: activeTanks.length > 0 ? 16 : 0 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <span className="section-title" style={{ margin: 0 }}>
            一键配置繁育
          </span>
          <button
            className="action-btn"
            onClick={selectAllIdle}
            style={{ padding: '4px 10px', fontSize: 12 }}
          >
            全选空闲
          </button>
        </div>

        <div className="tank-select-list">
          {TANKS.map(tank => {
            const isActive = countdowns[tank.id]?.active
            const isSelected = selectedTanks.includes(tank.id)
            return (
              <label
                key={tank.id}
                className={`tank-select-item ${isSelected ? 'selected' : ''} ${isActive ? 'disabled' : ''}`}
              >
                <input
                  type="checkbox"
                  checked={isSelected}
                  disabled={isActive}
                  onChange={() => !isActive && toggleTank(tank.id)}
                />
                <span className="tank-icon">🐠</span>
                <div className="tank-info">
                  <div className="tank-name">{tank.name}</div>
                  <div className="tank-status-text">
                    {isActive ? '繁育中' : '空闲'}
                  </div>
                </div>
              </label>
            )
          })}
        </div>

        <div style={{ marginTop: 16 }}>
          <div style={{ fontSize: 13, color: '#666', marginBottom: 8 }}>选择鱼种：</div>
          <div className="species-select">
            {SPECIES.map(s => (
              <button
                key={s.code}
                className={`species-btn ${selectedSpecies === s.code ? 'selected' : ''}`}
                onClick={() => setSelectedSpecies(s.code)}
              >
                <span className="species-icon">{s.icon}</span>
                <span>{s.name}</span>
              </button>
            ))}
          </div>
        </div>

        <div className="breeding-summary" style={{ marginTop: 16 }}>
          <div className="summary-row">
            <span>已选鱼缸</span>
            <span>{selectedTanks.length} 个</span>
          </div>
          <div className="summary-row">
            <span>目标水温</span>
            <span>26.5 ℃</span>
          </div>
          <div className="summary-row">
            <span>过滤泵</span>
            <span style={{ color: '#ff4d4f' }}>自动关闭 🚫</span>
          </div>
          <div className="summary-row">
            <span>产仔周期</span>
            <span>约 28 天</span>
          </div>
        </div>

        <button
          className="start-breeding-btn"
          disabled={selectedTanks.length === 0 || operating}
          onClick={handleBatchStart}
        >
          {operating ? '配置中...' : `一键启动繁育模式 (${selectedTanks.length}缸)`}
        </button>
      </div>

      <div className="card" style={{ marginTop: 12 }}>
        <div className="section-title" style={{ marginBottom: 8 }}>💡 繁育小贴士</div>
        <ul style={{ fontSize: 13, color: '#666', lineHeight: 1.8, paddingLeft: 16 }}>
          <li>繁育期自动关闭过滤泵，防止鱼苗被吸走</li>
          <li>水温提升至 26-28℃ 可加速产仔</li>
          <li>母鱼产仔后 24 小时内需捞出，避免吃苗</li>
          <li>鱼苗出生后 1 个月左右可移回大缸</li>
        </ul>
      </div>
    </div>
  )
}

function adjustCountdown(seconds) {
  if (seconds === undefined || seconds === null) return undefined
  return Math.max(0, seconds - 0)
}
