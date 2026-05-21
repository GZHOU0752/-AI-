<template>
  <div class="page-container">
    <div class="page-header">
      <h2>知识库问答</h2>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：知识库管理 -->
      <el-col :span="8">
        <el-card shadow="never" class="mb-16">
          <template #header>
            <span style="font-weight:600"><el-icon><Upload /></el-icon> 文档上传</span>
          </template>
          <el-upload
            drag
            :auto-upload="false"
            :on-change="handleFileChange"
            accept=".pdf,.docx,.doc,.txt,.md,.java,.xml,.json,.yaml,.yml"
          >
            <el-icon :size="36" color="#3b82f6"><UploadFilled /></el-icon>
            <div class="upload-text">拖拽或点击上传文档</div>
            <template #tip>
              <div class="upload-tip">支持 PDF、Word、Markdown、Java 等文件</div>
            </template>
          </el-upload>

          <div v-if="uploadFile" class="upload-file-info mt-8">
            <el-tag closable @close="uploadFile = null">{{ uploadFile.name }}</el-tag>
            <el-button type="primary" size="small" @click="doUpload" :loading="uploading" class="ml-8">
              入库
            </el-button>
          </div>

          <el-divider />

          <div class="kb-stats">
            <div class="kb-stat-item">
              <span class="kb-stat-label">检索策略</span>
              <el-tag size="small">混合检索</el-tag>
            </div>
            <div class="kb-stat-item">
              <span class="kb-stat-label">向量权重</span>
              <span style="font-weight:600">70%</span>
            </div>
            <div class="kb-stat-item">
              <span class="kb-stat-label">BM25 权重</span>
              <span style="font-weight:600">30%</span>
            </div>
            <div class="kb-stat-item">
              <span class="kb-stat-label">分块策略</span>
              <el-tag size="small" type="success">语义分块</el-tag>
            </div>
            <div class="kb-stat-item">
              <span class="kb-stat-label">文档解析</span>
              <el-tag size="small" type="warning">Apache Tika</el-tag>
            </div>
          </div>

          <el-divider />

          <div>
            <el-input
              v-model="searchQuery"
              placeholder="搜索知识库..."
              :prefix-icon="Search"
              clearable
              @keyup.enter="doSearch"
            >
              <template #append>
                <el-button @click="doSearch">搜索</el-button>
              </template>
            </el-input>
            <div v-if="searchResults.length" class="search-results mt-8">
              <el-card
                v-for="(item, i) in searchResults"
                :key="i"
                shadow="never"
                class="search-result-item"
              >
                <p>{{ item.content?.substring(0, 200) }}{{ item.content?.length > 200 ? '...' : '' }}</p>
              </el-card>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：问答对话 -->
      <el-col :span="16">
        <el-card shadow="never" class="chat-card">
          <template #header>
            <div class="chat-header">
              <div class="chat-header-left">
                <span style="font-weight:600"><el-icon><ChatDotRound /></el-icon> 智能问答</span>
                <el-tag size="small" class="session-id-tag">会话: {{ sessionId }}</el-tag>
                <el-tag size="small" type="info" class="session-id-tag">用户: {{ currentUserId }}</el-tag>
              </div>
              <div>
                <el-button text size="small" @click="showSessions = true">
                  <el-icon><Clock /></el-icon> 历史会话
                </el-button>
                <el-button text size="small" @click="newSession">新会话</el-button>
              </div>
            </div>
          </template>

          <!-- 消息列表 -->
          <div class="chat-messages" ref="chatContainer">
            <div v-if="messages.length === 0" class="chat-empty">
              <el-icon :size="48" color="#cbd5e1"><ChatLineSquare /></el-icon>
              <p>输入问题，AI将基于知识库和记忆为您解答</p>
            </div>

            <div
              v-for="(msg, i) in messages"
              :key="i"
              :class="['chat-message', msg.role === 'user' ? 'user-msg' : 'ai-msg']"
            >
              <div class="msg-avatar">
                <el-avatar :size="32" v-if="msg.role === 'user'">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <el-avatar :size="32" v-else style="background:#6366f1">
                  <el-icon><Cpu /></el-icon>
                </el-avatar>
              </div>
              <div class="msg-content">
                <div class="msg-role">{{ msg.role === 'user' ? '我' : 'AI助手' }}</div>
                <div class="msg-text" v-html="renderMarkdown(msg.content)"></div>
              </div>
            </div>

            <div v-if="answering" class="chat-message ai-msg">
              <div class="msg-avatar">
                <el-avatar :size="32" style="background:#6366f1">
                  <el-icon><Cpu /></el-icon>
                </el-avatar>
              </div>
              <div class="msg-content">
                <div class="msg-role">AI助手</div>
                <div class="msg-text typing">
                  <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入框 -->
          <div class="chat-input-area">
            <el-input
              v-model="question"
              type="textarea"
              :rows="2"
              placeholder="输入您的问题..."
              :disabled="answering"
              @keyup.enter.ctrl="doAsk"
            />
            <el-button
              type="primary"
              :icon="Promotion"
              :loading="answering"
              @click="doAsk"
              class="send-btn"
            >
              发送
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 历史会话抽屉 -->
    <el-drawer v-model="showSessions" title="历史会话" :size="360" direction="rtl">
      <div v-if="sessions.length === 0" class="session-empty">
        <p>暂无历史会话</p>
      </div>
      <div v-for="s in sessions" :key="s.sessionId" class="session-item" @click="loadSession(s.sessionId)">
        <div class="session-item-header">
          <span class="session-id">{{ s.sessionId }}</span>
        </div>
        <p class="session-preview">{{ s.preview || '(空会话)' }}</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="refreshSessions">刷新列表</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { Upload, UploadFilled, Search, ChatDotRound, ChatLineSquare, User, Cpu, Promotion, Clock } from '@element-plus/icons-vue'
import { knowledgeBase, memoryApi } from '@/api'
import { ElMessage } from 'element-plus'

// 稳定的用户标识：从 localStorage 获取或新建
const STORED_USER_KEY = 'ruima_user_id'
function getOrCreateUserId() {
  let uid = localStorage.getItem(STORED_USER_KEY)
  if (!uid) {
    uid = 'user_' + Date.now().toString(36) + '_' + Math.random().toString(36).substring(2, 8)
    localStorage.setItem(STORED_USER_KEY, uid)
  }
  return uid
}
const currentUserId = getOrCreateUserId()

const sessionId = ref('session_' + Date.now())
const question = ref('')
const messages = ref([])
const answering = ref(false)
const chatContainer = ref(null)

const showSessions = ref(false)
const sessions = ref([])
const uploadFile = ref(null)
const uploading = ref(false)
const searchQuery = ref('')
const searchResults = ref([])

function handleFileChange(file) {
  uploadFile.value = file.raw
}

async function doUpload() {
  if (!uploadFile.value) return
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadFile.value)
    await knowledgeBase.uploadFile(formData)
    ElMessage.success('文档解析入库成功')
    uploadFile.value = null
  } catch (e) {
    ElMessage.error('上传失败: ' + e.message)
  } finally {
    uploading.value = false
  }
}

async function doSearch() {
  if (!searchQuery.value.trim()) return
  try {
    const res = await knowledgeBase.search(searchQuery.value)
    searchResults.value = res || []
  } catch (e) {
    ElMessage.error('搜索失败')
  }
}

async function doAsk() {
  const q = question.value.trim()
  if (!q || answering.value) return

  messages.value.push({ role: 'user', content: q })
  question.value = ''
  answering.value = true

  await scrollToBottom()

  try {
    const res = await knowledgeBase.ask({
      sessionId: sessionId.value,
      userId: currentUserId,
      question: q
    })
    messages.value.push({ role: 'assistant', content: res.answer || '抱歉，无法回答此问题。' })
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '请求失败: ' + e.message })
  } finally {
    answering.value = false
    await scrollToBottom()
  }
}

async function refreshSessions() {
  try {
    const res = await knowledgeBase.listSessions(currentUserId)
    sessions.value = res || []
  } catch (e) { /* 忽略 */ }
}

async function loadSession(sid) {
  sessionId.value = sid
  showSessions.value = false
  messages.value = []
  try {
    const history = await memoryApi.getHistory(sid)
    if (history && history.length) {
      messages.value = history
    }
  } catch (e) { /* 忽略 */ }
}

function newSession() {
  sessionId.value = 'session_' + Date.now()
  messages.value = []
}

async function scrollToBottom() {
  await nextTick()
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

function renderMarkdown(text) {
  if (!text) return ''
  return text
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br/>')
    .replace(/- (.+)/g, '<li>$1</li>')
}

onMounted(async () => {
  try {
    const history = await memoryApi.getHistory(sessionId.value)
    if (history && history.length) {
      messages.value = history
    }
  } catch (e) { /* 忽略 */ }
  refreshSessions()
})
</script>

<style scoped>
.chat-card {
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
}
.chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chat-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.session-id-tag {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 11px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  margin-bottom: 16px;
  background: #f8fafc;
  border-radius: 12px;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-secondary);
}
.chat-empty p {
  margin-top: 12px;
}

.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.user-msg {
  flex-direction: row-reverse;
}
.msg-avatar {
  flex-shrink: 0;
}
.msg-content {
  max-width: 80%;
}
.msg-role {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.user-msg .msg-role {
  text-align: right;
}
.msg-text {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.user-msg .msg-text {
  background: #3b82f6;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.ai-msg .msg-text {
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-bottom-left-radius: 4px;
}

.typing .dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94a3b8;
  margin: 0 2px;
  animation: typing 1.4s infinite;
}
.typing .dot:nth-child(2) { animation-delay: 0.2s; }
.typing .dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.chat-input-area {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  flex-shrink: 0;
}
.chat-input-area :deep(.el-textarea__inner) {
  resize: none;
}
.send-btn {
  height: 60px;
  padding: 0 24px;
  flex-shrink: 0;
}

.upload-text {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 8px;
}
.upload-tip {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.kb-stats {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.kb-stat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}
.kb-stat-label {
  color: var(--text-secondary);
}

.search-results {
  max-height: 240px;
  overflow-y: auto;
}
.search-result-item {
  margin-bottom: 8px;
  font-size: 13px;
}
.search-result-item p {
  color: var(--text-secondary);
  line-height: 1.5;
}

.session-empty {
  text-align: center;
  padding: 48px 0;
  color: var(--text-secondary);
}
.session-item {
  padding: 14px;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  border: 1px solid var(--border-color);
  transition: background 0.2s;
}
.session-item:hover {
  background: #e8f2fc;
  border-color: var(--primary-color);
}
.session-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.session-id {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}
.session-preview {
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mb-16 { margin-bottom: 16px; }
.mt-8 { margin-top: 8px; }
.ml-8 { margin-left: 8px; }
</style>
