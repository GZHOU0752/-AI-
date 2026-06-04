import { reactive } from 'vue'

const AUTH_KEY = 'ruima_auth'

// 从 localStorage 恢复登录状态
function loadAuth() {
  try {
    const raw = localStorage.getItem(AUTH_KEY)
    if (raw) return JSON.parse(raw)
  } catch (e) { /* ignore */ }
  return null
}

function saveAuth(state) {
  localStorage.setItem(AUTH_KEY, JSON.stringify(state))
}

function clearAuth() {
  localStorage.removeItem(AUTH_KEY)
}

// 全局响应式认证状态
const state = reactive({
  loggedIn: false,
  userId: null,
  username: null,

  // 初始化时尝试恢复登录
  ...loadAuth(),
})

export function useAuth() {
  function login(user) {
    state.loggedIn = true
    state.userId = user.userId
    state.username = user.username
    saveAuth({ loggedIn: true, userId: user.userId, username: user.username })
  }

  function logout() {
    state.loggedIn = false
    state.userId = null
    state.username = null
    clearAuth()
  }

  return { state, login, logout }
}
