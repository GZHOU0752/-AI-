import axios from 'axios'

const api = axios.create({
  baseURL: '/ruima-ai',
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const msg = err.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

// ==================== AI Code Review ====================
export const aiCodeReview = {
  review(data) {
    return api.post('/api/aicr/review', data)
  },
  reviewFiles(formData) {
    return api.post('/api/aicr/review/files', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
  },
  health() {
    return api.get('/api/aicr/health')
  }
}

// ==================== 知识库 ====================
export const knowledgeBase = {
  ask({ sessionId = 'default', userId = 'anonymous', question }) {
    return api.post('/api/kb/ask', { sessionId, userId, question })
  },
  getContext(sessionId) {
    return api.get(`/api/kb/context/${sessionId}`)
  },
  listSessions(userId) {
    return api.get('/api/kb/sessions', { params: { userId } })
  },
  uploadDocument(data) {
    return api.post('/api/kb/documents/upload', data)
  },
  uploadFile(formData) {
    return api.post('/api/kb/documents/upload/file', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  search(query) {
    return api.post('/api/kb/search', { query })
  }
}

// ==================== 记忆管理 ====================
export const memoryApi = {
  getHistory(sessionId) {
    return api.get(`/api/memory/history/${sessionId}`)
  },
  addMessage(data) {
    return api.post('/api/memory/message', data)
  },
  generateSummary(sessionId) {
    return api.post(`/api/memory/summary/${sessionId}`)
  },
  storeLongTerm({ userId, content, category }) {
    return api.post('/api/memory/long-term/store', { userId, content, category })
  },
  recallLongTerm({ userId, query }) {
    return api.post('/api/memory/long-term/recall', { userId, query })
  }
}

// ==================== 单元测试 ====================
export const unitTest = {
  generate(data) {
    return api.post('/api/test/generate', data)
  },
  generateFromFile(formData) {
    return api.post('/api/test/generate/file', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
  },
  analyzeCoverage(data) {
    return api.post('/api/test/coverage/analyze', data)
  },
  analyzeFromFiles(formData) {
    return api.post('/api/test/coverage/analyze/file', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
  },
  generateReport(data) {
    return api.post('/api/test/coverage/report', data)
  }
}
