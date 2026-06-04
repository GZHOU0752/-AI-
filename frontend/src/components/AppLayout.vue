<template>
  <el-container class="app-layout">
    <!-- Sidebar -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="app-sidebar">
      <div class="sidebar-brand" @click="router.push('/code-review')">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
          <rect width="24" height="24" rx="6" fill="#00b96b"/>
          <path d="M7 7.5C7 7.5 8.5 5.5 12 5.5C15.5 5.5 17 7 17 7.5V16.5C17 17 15.5 18.5 12 18.5C8.5 18.5 7 17 7 16.5V7.5Z" fill="white" opacity="0.95"/>
        </svg>
        <span v-show="!isCollapse" class="brand-name">睿码AI中心</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        class="sidebar-nav"
      >
        <el-menu-item index="/code-review">
          <el-icon><DocumentChecked /></el-icon>
          <template #title>AI 代码评审</template>
        </el-menu-item>
        <el-menu-item index="/knowledge-base">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>知识库问答</template>
        </el-menu-item>
        <el-menu-item index="/memory">
          <el-icon><Connection /></el-icon>
          <template #title>记忆管理</template>
        </el-menu-item>
        <el-menu-item index="/unit-test">
          <el-icon><Notebook /></el-icon>
          <template #title>单元测试</template>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer" v-show="!isCollapse && authState.loggedIn">
        <div class="footer-user">
          <span class="footer-avatar">{{ authState.username?.charAt(0) }}</span>
          <span class="footer-name">{{ authState.username }}</span>
        </div>
        <button class="footer-action" @click="showPwd=true">修改密码</button>
      </div>
    </el-aside>

    <!-- Main -->
    <el-container class="app-main">
      <el-header class="app-header">
        <button class="toggle-btn" @click="isCollapse = !isCollapse">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" :class="{ flipped: isCollapse }">
            <path d="M6 3L11 8L6 13" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <div class="header-right">
          <span class="status-indicator" :class="apiOnline ? 'ok' : 'fail'"></span>
          <span class="status-label">{{ apiOnline ? '正常' : '离线' }}</span>
          <span class="header-sep"></span>
          <span class="header-clock">{{ currentTime }}</span>
          <span class="header-sep"></span>
          <button class="logout-link" v-if="authState.loggedIn" @click="doLogout">退出</button>
        </div>
      </el-header>

      <el-main class="app-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <el-dialog v-model="showPwd" title="修改密码" width="360px" :close-on-click-modal="false" center>
      <el-form label-position="top">
        <el-form-item label="原密码"><el-input v-model="pwd.old" type="password" size="large" maxlength="6" /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwd.new1" type="password" size="large" maxlength="6" placeholder="6位数字" /></el-form-item>
        <el-form-item label="确认新密码"><el-input v-model="pwd.new2" type="password" size="large" maxlength="6" /></el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="pwdLoading" @click="doChangePwd">确认修改</el-button>
      </el-form>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { DocumentChecked, ChatDotRound, Connection, Notebook } from '@element-plus/icons-vue'
import { useAuth } from '@/stores/auth'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const { state: authState, logout } = useAuth()

const isCollapse = ref(false)
const apiOnline = ref(true)
const currentTime = ref('')

let timer = null
const activeMenu = computed(() => route.path)

onMounted(() => {
  tick()
  timer = setInterval(tick, 30000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })

function tick() {
  const d = new Date()
  currentTime.value = d.toLocaleDateString('zh-CN', { month:'2-digit', day:'2-digit' }) + ' ' +
    d.toLocaleTimeString('zh-CN', { hour:'2-digit', minute:'2-digit', second:'2-digit' })
}

function doLogout() { logout(); router.push('/login') }

const showPwd = ref(false)
const pwdLoading = ref(false)
const pwd = reactive({ old: '', new1: '', new2: '' })
async function doChangePwd() {
  if (!pwd.old || !pwd.new1) { ElMessage.warning('请填写完整'); return }
  if (pwd.new1.length !== 6) { ElMessage.warning('新密码必须为6位'); return }
  if (pwd.new1 !== pwd.new2) { ElMessage.warning('两次输入不一致'); return }
  pwdLoading.value = true
  try {
    await authApi.changePassword(authState.userId, pwd.old, pwd.new1)
    ElMessage.success('密码修改成功，请重新登录')
    logout(); router.push('/login')
  } catch(e) { ElMessage.error(e.message || '修改失败') }
  finally { pwdLoading.value = false; pwd = { old: '', new1: '', new2: '' } }
}
</script>

<style scoped>
.app-layout { height: 100dvh; overflow: hidden; }

/* Sidebar */
.app-sidebar {
  background: var(--color-surface);
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border);
  transition: width 0.2s var(--ease);
  overflow: hidden;
}

.sidebar-brand {
  height: 52px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  cursor: pointer;
  flex-shrink: 0;
}

.brand-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  white-space: nowrap;
  letter-spacing: 0;
}

.sidebar-nav {
  flex: 1;
  border-right: none !important;
  padding: 6px 10px;
  overflow-y: auto;
}

:deep(.sidebar-nav .el-menu-item) {
  margin: 1px 0;
  border-radius: var(--radius-sm);
  height: 38px;
  line-height: 38px;
  font-size: 13px;
  color: var(--color-text-secondary);
  transition: all 0.12s var(--ease);
}

:deep(.sidebar-nav .el-menu-item:hover) {
  background: var(--color-brand-light) !important;
  color: var(--color-brand) !important;
}

:deep(.sidebar-nav .el-menu-item.is-active) {
  background: var(--color-brand-light) !important;
  color: var(--color-brand) !important;
  font-weight: 600;
}

.sidebar-footer {
  padding: 12px 18px 16px;
  border-top: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.footer-user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.footer-avatar {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: var(--color-brand);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.footer-name {
  font-size: 13px;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.footer-action {
  width: 100%;
  border: none;
  background: var(--color-border-light);
  padding: 5px 0;
  border-radius: 5px;
  font-size: 12px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all .15s;
  font-family: var(--font-sans);
}
.footer-action:hover { background: var(--color-brand-light); color: var(--color-brand); }

/* Header */
.app-main { flex-direction: column; overflow: hidden; background: var(--color-bg); }

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  padding: 0 20px;
  height: 48px;
  flex-shrink: 0;
}

.toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.toggle-btn:hover {
  background: var(--color-border-light);
  color: var(--color-text);
}

.toggle-btn svg {
  transition: transform 0.2s var(--ease);
}

.toggle-btn svg.flipped {
  transform: rotate(180deg);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
}

.status-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-indicator.ok { background: var(--color-success); }
.status-indicator.fail { background: var(--color-danger); }

.status-label { color: var(--color-text-secondary); }

.header-sep {
  width: 1px;
  height: 14px;
  background: var(--color-border);
}

.header-clock {
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}

.logout-link {
  border: none;
  background: none;
  font-size: 12px;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: var(--font-sans);
}
.logout-link:hover { color: var(--color-danger); background: var(--color-danger-bg); }

.app-content { overflow-y: auto; }
</style>
