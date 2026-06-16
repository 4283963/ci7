import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.response.use(
  (res) => {
    if (res.data.code === 200) {
      return res.data.data
    }
    return Promise.reject(new Error(res.data.message || 'Request failed'))
  },
  (err) => Promise.reject(err)
)

export const waterQualityApi = {
  getLatest: (tankId) => api.get(`/water-quality/latest/${tankId}`),
  getAllLatest: () => api.get('/water-quality/latest'),
}

export const deviceApi = {
  getByTank: (tankId) => api.get(`/devices/tank/${tankId}`),
  getDevice: (deviceId) => api.get(`/devices/${deviceId}`),
  sendCommand: (deviceId, body) => api.post(`/devices/${deviceId}/command`, body),
  toggleDevice: (deviceId, action) => api.post(`/devices/${deviceId}/toggle?action=${action}`),
}

export const alertApi = {
  getByTank: (tankId) => api.get(`/alerts/tank/${tankId}`),
  getCritical: (tankId) => api.get(`/alerts/critical/${tankId}`),
  getByLevel: (level) => api.get(`/alerts/level/${level}`),
}

export const historyApi = {
  getByTank: (tankId, params) => api.get(`/history/tank/${tankId}`, { params }),
  getLatest: (tankId, limit) => api.get(`/history/tank/${tankId}/latest`, { params: { limit } }),
}

export const iotApi = {
  sendCommand: (body) => api.post('/iot/command', body),
  getCommandHistory: (tankId) => api.get(`/iot/commands/tank/${tankId}`),
}

export const breedingApi = {
  getAllConfigs: () => api.get('/breeding/configs'),
  getConfig: (tankId) => api.get(`/breeding/config/${tankId}`),
  updateConfig: (tankId, config) => api.put(`/breeding/config/${tankId}`, config),
  startBreeding: (tankId, species) => api.post(`/breeding/start/${tankId}?species=${species}`),
  stopBreeding: (tankId) => api.post(`/breeding/stop/${tankId}`),
  markAsBirthed: (tankId) => api.post(`/breeding/mark-birthed/${tankId}`),
  getCountdown: (tankId) => api.get(`/breeding/countdown/${tankId}`),
  getAllCountdowns: () => api.get('/breeding/countdown'),
  getActiveBreeding: () => api.get('/breeding/active'),
  getSpeciesPresets: () => api.get('/breeding/species'),
  batchStart: (tankIds, species) => api.post('/breeding/batch-start', { tankIds, species }),
  batchStop: (tankIds) => api.post('/breeding/batch-stop', tankIds),
}

export default api
