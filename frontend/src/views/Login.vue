<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
          <rect width="40" height="40" rx="10" fill="#00b96b"/>
          <path d="M11 13C11 13 14 9 20 9C26 9 29 12 29 13V28C29 29 26 32 20 32C14 32 11 29 11 28V13Z" fill="white" opacity="0.95"/>
        </svg>
        <div>
          <h1>睿码AI中心</h1>
          <p>AI 驱动的研发管理平台</p>
        </div>
      </div>

      <el-form :model="form" label-position="top">
        <el-form-item label="用户名">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            @keyup.enter="doLogin"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入 6 位密码"
            size="large"
            maxlength="6"
            show-password
            @keyup.enter="doLogin"
          />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="login-btn"
          :loading="loginLoading"
          @click="doLogin"
        >登 录</el-button>
      </el-form>

      <p class="register-link">
        没有账号？<a @click="showRegister = true">立即注册</a>
      </p>
    </div>

    <!-- Register dialog -->
    <el-dialog v-model="showRegister" title="创建账号" width="400px" :close-on-click-modal="false" center>
      <el-form :model="regForm" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="regForm.username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="regForm.password" type="password" placeholder="请输入 6 位密码" size="large" maxlength="6" show-password />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="regLoading" @click="doRegister">注 册</el-button>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api'
import { useAuth } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const { login } = useAuth()

const form = reactive({ username: '', password: '' })
const loginLoading = ref(false)
const showRegister = ref(false)
const regForm = reactive({ username: '', password: '' })
const regLoading = ref(false)

async function doLogin() {
  if (!form.username || !form.password) { ElMessage.warning('请填写用户名和密码'); return }
  loginLoading.value = true
  try {
    const res = await authApi.login(form.username, form.password)
    login(res)
    router.push('/code-review')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally { loginLoading.value = false }
}

async function doRegister() {
  if (!regForm.username || regForm.password.length !== 6) { ElMessage.warning('密码必须为 6 位'); return }
  regLoading.value = true
  try {
    const res = await authApi.register(regForm.username, regForm.password)
    login(res)
    showRegister.value = false
    router.push('/code-review')
  } catch (e) {
    ElMessage.error(e.message || '注册失败')
  } finally { regLoading.value = false }
}
</script>

<style scoped>
.login-page {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
  padding: 24px;
}

.login-card {
  width: 400px;
  padding: 40px 36px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.login-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 36px;
}

.login-header h1 {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
  line-height: 1.3;
  letter-spacing: 0;
}

.login-header p {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 2px;
}

:deep(.el-form-item__label) {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
}

.login-btn {
  width: 100%;
  margin-top: 4px;
  height: 44px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.register-link {
  text-align: center;
  margin-top: 24px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.register-link a {
  color: var(--color-brand);
  cursor: pointer;
  font-weight: 500;
}

.register-link a:hover {
  text-decoration: underline;
}

@media (max-width: 440px) {
  .login-card { width: 100%; padding: 32px 24px; }
}
</style>
