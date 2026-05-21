<template>
  <div class="page-container">
    <div class="page-header">
      <h2>记忆管理</h2>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：短期记忆 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-title">
              <span><el-icon color="#3b82f6"><Timer /></el-icon> 短期记忆</span>
              <el-tag size="small" type="primary">滑动窗口 | Redis</el-tag>
            </div>
          </template>

          <div class="card-toolbar">
            <el-input v-model="stSessionId" placeholder="会话ID" size="small" style="width:180px" />
            <el-button type="primary" size="small" @click="loadShortTerm">加载历史</el-button>
            <el-button size="small" @click="generateSummary">生成摘要</el-button>
          </div>

          <el-alert
            v-if="stSummary"
            :title="'会话摘要'"
            :description="stSummary"
            type="info"
            :closable="true"
            @close="stSummary = ''"
            class="mb-12"
          />

          <div v-if="stMessages.length === 0" class="empty-block">
            <p>暂无对话历史，输入会话ID加载</p>
          </div>

          <div class="memory-list">
            <div
              v-for="(msg, i) in stMessages"
              :key="i"
              :class="['memory-item', msg.role === 'user' ? 'user' : 'assistant']"
            >
              <div class="memory-item-header">
                <el-tag :type="msg.role === 'user' ? '' : 'info'" size="small">
                  {{ msg.role === 'user' ? '用户' : 'AI' }}
                </el-tag>
                <span class="memory-time">{{ formatTime(msg.timestamp) }}</span>
              </div>
              <p class="memory-text">{{ msg.content?.substring(0, 200) }}{{ msg.content?.length > 200 ? '...' : '' }}</p>
            </div>
          </div>

          <el-divider />

          <div class="add-memory-form">
            <el-input
              v-model="newStMessage"
              placeholder="手动添加消息..."
              size="small"
            />
            <el-button-group class="mt-8">
              <el-button size="small" @click="addMessage('user')">添加用户消息</el-button>
              <el-button size="small" type="primary" @click="addMessage('assistant')">添加AI消息</el-button>
            </el-button-group>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：长期记忆 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-title">
              <span><el-icon color="#6366f1"><Coin /></el-icon> 长期记忆</span>
              <el-tag size="small" type="info">Pinecone | 向量检索</el-tag>
            </div>
          </template>

          <!-- 存储 -->
          <div class="section-title">存储新记忆</div>
          <div class="add-memory-form">
            <el-input v-model="ltUserId" placeholder="用户ID" size="small" class="mb-8" />
            <el-input
              v-model="ltContent"
              type="textarea"
              :rows="3"
              placeholder="记忆内容..."
              size="small"
            />
            <el-select v-model="ltCategory" placeholder="分类" size="small" class="mt-8" style="width:100%">
              <el-option label="技术文档" value="tech_doc" />
              <el-option label="问答记录" value="qa_history" />
              <el-option label="代码审查" value="code_review" />
              <el-option label="项目规范" value="project_rule" />
              <el-option label="其他" value="general" />
            </el-select>
            <el-button type="primary" size="small" class="mt-8" :loading="storing" @click="storeLongTerm" style="width:100%">
              存入长期记忆
            </el-button>
          </div>

          <el-divider />

          <!-- 召回 -->
          <div class="section-title">语义相似度召回</div>
          <div class="add-memory-form">
            <el-input v-model="ltQuery" placeholder="输入查询内容..." size="small" />
            <el-button type="primary" size="small" class="mt-8" :loading="recalling" @click="recallLongTerm" style="width:100%">
              搜索记忆
            </el-button>
          </div>

          <div v-if="ltResults.length" class="recall-results mt-12">
            <div class="section-title">召回结果 (相似度 ≥ 75%)</div>
            <div v-for="(item, i) in ltResults" :key="i" class="recall-item">
              <div class="recall-index">#{{ i + 1 }}</div>
              <p>{{ item }}</p>
            </div>
          </div>

          <div v-if="ltResults.length === 0 && recalled" class="empty-block mt-12">
            <p>未找到相关记忆</p>
          </div>

        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Timer, Coin } from '@element-plus/icons-vue'
import { memoryApi } from '@/api'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

// 短期记忆
const stSessionId = ref('default')
const stMessages = ref([])
const newStMessage = ref('')
const stSummary = ref('')

// 长期记忆
const ltUserId = ref('user_001')
const ltContent = ref('')
const ltCategory = ref('general')
const ltQuery = ref('')
const ltResults = ref([])
const storing = ref(false)
const recalling = ref(false)
const recalled = ref(false)

function formatTime(ts) {
  if (!ts) return ''
  return dayjs(ts).format('MM-DD HH:mm:ss')
}

async function loadShortTerm() {
  try {
    const res = await memoryApi.getHistory(stSessionId.value)
    stMessages.value = res || []
    ElMessage.success(`加载了 ${stMessages.value.length} 条消息`)
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

async function addMessage(role) {
  if (!newStMessage.value.trim()) return
  try {
    await memoryApi.addMessage({
      role,
      content: newStMessage.value,
      sessionId: stSessionId.value
    })
    newStMessage.value = ''
    await loadShortTerm()
  } catch (e) {
    ElMessage.error('添加失败')
  }
}

async function generateSummary() {
  try {
    const res = await memoryApi.generateSummary(stSessionId.value)
    stSummary.value = res.summary || res
  } catch (e) {
    ElMessage.error('生成摘要失败')
  }
}

async function storeLongTerm() {
  if (!ltContent.value.trim()) {
    ElMessage.warning('请输入记忆内容')
    return
  }
  storing.value = true
  try {
    await memoryApi.storeLongTerm({
      userId: ltUserId.value,
      content: ltContent.value,
      category: ltCategory.value
    })
    ElMessage.success('记忆已存储')
    ltContent.value = ''
  } catch (e) {
    ElMessage.error('存储失败')
  } finally {
    storing.value = false
  }
}

async function recallLongTerm() {
  if (!ltQuery.value.trim()) return
  recalling.value = true
  recalled.value = false
  ltResults.value = []
  try {
    const res = await memoryApi.recallLongTerm({
      userId: ltUserId.value,
      query: ltQuery.value
    })
    ltResults.value = res.memories || []
    recalled.value = true
  } catch (e) {
    ElMessage.error('召回失败')
  } finally {
    recalling.value = false
  }
}
</script>

<style scoped>
.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
.card-title span {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.memory-list {
  max-height: 360px;
  overflow-y: auto;
}
.memory-item {
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 8px;
}
.memory-item.user {
  background: #eff6ff;
  border-left: 3px solid #3b82f6;
}
.memory-item.assistant {
  background: #f5f3ff;
  border-left: 3px solid #6366f1;
}
.memory-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.memory-time {
  font-size: 11px;
  color: var(--text-secondary);
}
.memory-text {
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-primary);
}

.add-memory-form {
  display: flex;
  flex-direction: column;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-block {
  text-align: center;
  padding: 48px 0;
  color: var(--text-secondary);
}

.recall-results {
  max-height: 240px;
  overflow-y: auto;
}
.recall-item {
  display: flex;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-color);
}
.recall-index {
  font-weight: 700;
  color: #6366f1;
  flex-shrink: 0;
}
.recall-item p {
  font-size: 13px;
  line-height: 1.5;
}

.mb-8 { margin-bottom: 8px; }
.mb-12 { margin-bottom: 12px; }
.mt-8 { margin-top: 8px; }
.mt-12 { margin-top: 12px; }
</style>
