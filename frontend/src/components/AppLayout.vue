<template>
  <el-container class="app-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="app-sidebar">
      <div class="sidebar-header">
        <div class="logo" @click="router.push('/code-review')">
          <el-icon :size="24"><Cpu /></el-icon>
          <span v-show="!isCollapse" class="logo-text">睿码AI中心</span>
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        background-color="transparent"
        text-color="#64748b"
        active-text-color="#4a90d9"
        class="sidebar-menu"
      >
        <el-menu-item index="/code-review">
          <el-icon><DocumentChecked /></el-icon>
          <template #title>AI代码评审</template>
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
    </el-aside>

    <!-- 主内容 -->
    <el-container class="app-main">
      <el-header class="app-header">
        <div class="header-left">
          <el-button
            text
            @click="isCollapse = !isCollapse"
            class="collapse-btn"
          >
            <el-icon :size="20">
              <Fold v-if="!isCollapse" />
              <Expand v-else />
            </el-icon>
          </el-button>
        </div>
        <div class="header-right">
          <el-tooltip content="API 状态" placement="bottom">
            <el-tag :type="apiOnline ? 'success' : 'danger'" size="small" effect="dark">
              {{ apiOnline ? '服务在线' : '服务离线' }}
            </el-tag>
          </el-tooltip>
          <el-divider direction="vertical" />
          <span class="header-time">{{ currentTime }}</span>
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
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()

const isCollapse = ref(false)
const apiOnline = ref(true)
const currentTime = ref('')

let timer = null

const activeMenu = computed(() => {
  return route.path
})

onMounted(() => {
  timer = setInterval(() => {
    currentTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.app-layout {
  height: 100vh;
  overflow: hidden;
}

.app-sidebar {
  background: #ffffff;
  overflow: hidden;
  transition: width 0.3s;
  border-right: 1px solid var(--border-color);
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--primary-color);
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
  color: var(--primary-color);
}

.sidebar-menu {
  border-right: none;
  margin-top: 8px;
}

.sidebar-menu .el-menu-item {
  margin: 4px 8px;
  border-radius: 8px;
}

.sidebar-menu .el-menu-item:hover {
  background-color: #e8f2fc !important;
}

.sidebar-menu .el-menu-item.is-active {
  background-color: #d0e7ff !important;
  color: var(--primary-color) !important;
  font-weight: 600;
}

.app-main {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid var(--border-color);
  padding: 0 24px;
  height: 56px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  padding: 8px;
  font-size: 18px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-time {
  font-size: 13px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}

.app-content {
  background: var(--bg-color);
  overflow-y: auto;
  padding: 0;
}
</style>
