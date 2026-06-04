<template>
  <div class="page-container">
    <div class="page-header">
      <h2>知识库问答</h2>
      <p>基于团队文档与上下文记忆的智能助手</p>
    </div>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="never" class="side-card">
          <template #header><span class="card-title">知识库管理</span></template>

          <div class="section">
            <div class="sec-label">上传文档</div>
            <el-upload drag :auto-upload="false" :on-change="handleFile" accept=".pdf,.docx,.doc,.txt,.md,.java,.xml,.json,.yaml,.yml">
              <div class="drop-zone">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                  <path d="M12 4V16M12 4L8 8M12 4L16 8" stroke="#8f959e" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M4 16V19C4 19.5523 4.44772 20 5 20H19C19.5523 20 20 19.5523 20 19V16" stroke="#8f959e" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                <span class="drop-text">拖拽或点击上传</span>
              </div>
            </el-upload>
            <div v-if="uploadFile" class="upload-bar">
              <span class="upload-name">{{ uploadFile.name }}</span>
              <el-button type="primary" size="small" :loading="uploading" @click="doUpload">入库</el-button>
            </div>
          </div>

          <el-divider />

          <div class="section">
            <div class="sec-label">搜索知识库</div>
            <el-input v-model="sq" placeholder="输入关键词..." size="small" clearable @keyup.enter="doSearch">
              <template #prefix>
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                  <circle cx="5.5" cy="5.5" r="4" stroke="#8f959e" stroke-width="1.2"/>
                  <path d="M8.5 8.5L12 12" stroke="#8f959e" stroke-width="1.2" stroke-linecap="round"/>
                </svg>
              </template>
            </el-input>
            <div v-if="searchResults.length" class="search-list">
              <div v-for="(r,i) in searchResults" :key="i" class="search-item">
                {{ r.content?.substring(0,140) }}{{ r.content?.length>140?'...':'' }}
              </div>
            </div>
          </div>

          <el-divider />

          <div class="section">
            <div class="sec-label">已上传文档 ({{ docs.length }})</div>
            <div v-if="!docs.length" class="no-docs">暂无文档</div>
            <div v-for="d in docs" :key="d.id" class="doc-item">
              <span class="doc-name">{{ d.title }}</span>
              <el-button text size="small" type="danger" @click="delDoc(d.id)">删除</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card shadow="never" class="chat-card">
          <template #header>
            <div class="chat-head">
              <span class="card-title">智能问答</span>
              <div class="chat-head-right">
                <span class="tag">{{ authState.username }}</span>
                <span class="tag mono">{{ sidShort }}</span>
                <el-button text size="small" @click="showSessions=true">历史</el-button>
                <el-button text size="small" @click="newSession">新会话</el-button>
              </div>
            </div>
          </template>

          <div class="chat-msgs" ref="chatRef">
            <div v-if="!messages.length" class="chat-empty">输入问题开始对话</div>
            <div v-for="(m,i) in messages" :key="i" :class="['msg', m.role==='user'?'msg-me':'msg-ai']">
              <div class="bubble" v-html="md(m.content)"></div>
            </div>
            <div v-if="answering" class="msg msg-ai">
              <div class="bubble typing"><i></i><i></i><i></i></div>
            </div>
          </div>

          <div class="chat-foot">
            <div class="input-row">
              <el-input v-model="q" placeholder="输入问题..." :disabled="answering" @keyup.enter.ctrl="doAsk" size="large" />
              <el-button type="primary" :loading="answering" @click="doAsk" class="send-btn">发送</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-drawer v-model="showSessions" title="历史会话" :size="360" direction="rtl">
      <div v-if="!sessions.length" class="empty-sm">暂无历史会话</div>
      <div v-for="s in sessions" :key="s.sessionId" class="sess-item" @click="loadSession(s.sessionId)">
        <div class="sess-id">{{ s.sessionId }}</div>
        <p class="sess-preview">{{ s.preview||'(空会话)' }}</p>
      </div>
      <template #footer><el-button type="primary" @click="refreshSessions" style="width:100%">刷新</el-button></template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { knowledgeBase, memoryApi } from '@/api'
import { useAuth } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const { state: authState } = useAuth()
const sid = ref('session_'+Date.now())
const q = ref('')
const messages = ref([])
const answering = ref(false)
const chatRef = ref(null)
const showSessions = ref(false)
const sessions = ref([])
const uploadFile = ref(null)
const uploading = ref(false)
const sq = ref('')
const searchResults = ref([])
const docs = ref([])

const sidShort = computed(() => { const s = sid.value; return s.length>24?s.slice(0,10)+'...'+s.slice(-8):s })

function handleFile(f) { uploadFile.value = f.raw }
async function loadDocs(){try{docs.value=await knowledgeBase.listDocuments()||[]}catch(e){}}
async function delDoc(id){try{await knowledgeBase.deleteDocument(id);ElMessage.success('已删除');loadDocs()}catch(e){ElMessage.error('删除失败')}}
async function doUpload() {
  if (!uploadFile.value) return; uploading.value = true
  try { const fd = new FormData(); fd.append('file',uploadFile.value); await knowledgeBase.uploadFile(fd); ElMessage.success('入库成功'); uploadFile.value=null; loadDocs() }
  catch(e) { ElMessage.error('失败: '+e.message) } finally { uploading.value=false }
}
async function doSearch() { if(!sq.value.trim())return; try{searchResults.value=await knowledgeBase.search(sq.value)||[]}catch(e){ElMessage.error('搜索失败')} }
async function doAsk() {
  const t = q.value.trim(); if(!t||answering.value)return
  messages.value.push({role:'user',content:t}); q.value=''; answering.value=true; await scroll()
  try {
    const r = await knowledgeBase.ask({ sessionId:sid.value, userId:authState.userId, question:t })
    messages.value.push({role:'assistant',content:r.answer||'抱歉'})
  } catch(e) { messages.value.push({role:'assistant',content:'请求失败: '+e.message}) }
  finally { answering.value=false; await scroll() }
}
async function refreshSessions() { try { sessions.value = await knowledgeBase.listSessions(authState.userId)||[] } catch(e){} }
async function loadSession(id) { sid.value=id; showSessions.value=false; messages.value=[]; try{const h=await memoryApi.getHistory(id); if(h?.length)messages.value=h}catch(e){} }
function newSession() { sid.value='session_'+Date.now(); messages.value=[] }
async function scroll() { await nextTick(); if(chatRef.value)chatRef.value.scrollTop=chatRef.value.scrollHeight }
function md(t) { return t?t.replace(/```(\w*)\n([\s\S]*?)```/g,'<pre><code>$2</code></pre>').replace(/\*\*(.+?)\*\*/g,'<strong>$1</strong>').replace(/\n/g,'<br/>'):'' }
onMounted(async ()=>{ try{const h=await memoryApi.getHistory(sid.value); if(h?.length)messages.value=h}catch(e){}; refreshSessions(); loadDocs() })
</script>

<style scoped>
.card-title { font-weight: 600; font-size: 14px; }
.side-card { height: calc(100dvh - 150px); }
.side-card :deep(.el-card__body) { overflow-y: auto; }

.section { margin-bottom: 4px; }
.sec-label { font-size: 12px; font-weight: 500; color: var(--color-text-secondary); margin-bottom: 8px; }

.drop-zone { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 10px 0; }
.drop-text { font-size: 12px; color: var(--color-text-muted); }

.upload-bar { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; padding: 8px 10px; background: var(--color-border-light); border-radius: var(--radius-sm); }
.upload-name { font-size: 12px; color: var(--color-text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 160px; }

.search-list { max-height: 220px; overflow-y: auto; margin-top: 8px; }
.search-item { padding: 8px 10px; font-size: 12px; color: var(--color-text-secondary); background: var(--color-border-light); border-radius: var(--radius-sm); margin-bottom: 4px; line-height: 1.5; }

/* Chat */
.chat-card { height: calc(100dvh - 150px); display: flex; flex-direction: column; }
.chat-card :deep(.el-card__body) { flex:1; display:flex; flex-direction:column; overflow:hidden; padding:0!important; }
.chat-head { display: flex; align-items: center; justify-content: space-between; }
.chat-head-right { display: flex; align-items: center; gap: 6px; }
.tag { font-size: 11px; padding: 2px 6px; border-radius: 4px; background: var(--color-border-light); color: var(--color-text-muted); }
.tag.mono { font-family: var(--font-mono); }

.chat-msgs { flex: 1; overflow-y: auto; padding: 16px 20px; background: var(--color-bg); }
.chat-empty { display: flex; align-items: center; justify-content: center; height: 100%; color: var(--color-text-muted); font-size: 14px; }

.msg { margin-bottom: 16px; display: flex; }
.msg-me { justify-content: flex-end; }
.bubble { max-width: 76%; padding: 10px 14px; border-radius: 10px; font-size: 13px; line-height: 1.65; word-break: break-word; }
.msg-me .bubble { background: var(--color-brand); color: #fff; border-bottom-right-radius: 4px; }
.msg-ai .bubble { background: var(--color-surface); border: 1px solid var(--color-border); border-bottom-left-radius: 4px; }
.bubble :deep(pre) { background: rgba(0,0,0,0.05); padding: 8px 10px; border-radius: 6px; font-size: 12px; overflow-x: auto; margin: 4px 0; font-family: var(--font-mono); }
.msg-me .bubble :deep(pre) { background: rgba(255,255,255,0.15); }

.typing { display: flex; gap: 3px; padding: 12px 16px; }
.typing i { width: 5px; height: 5px; border-radius: 50%; background: #c0c4cc; animation: dot 1.2s infinite; display: block; }
.typing i:nth-child(2){animation-delay:.15s}.typing i:nth-child(3){animation-delay:.3s}
@keyframes dot{0%,60%,100%{opacity:.3}30%{opacity:1}}

.chat-foot { padding: 12px 16px; border-top: 1px solid var(--color-border); background: var(--color-surface); flex-shrink: 0; }
.input-row { display: flex; gap: 10px; }
.send-btn { height: 40px; flex-shrink: 0; padding: 0 20px; font-weight: 600; }

.no-docs { text-align:center;padding:16px 0;color:var(--color-text-muted);font-size:12px; }
.doc-item { display:flex;align-items:center;justify-content:space-between;padding:6px 0;border-bottom:1px solid var(--color-border-light); }
.doc-name { font-size:12px;color:var(--color-text-secondary);overflow:hidden;text-overflow:ellipsis;white-space:nowrap;flex:1; }

.empty-sm { text-align: center; padding: 48px 0; color: var(--color-text-muted); font-size: 13px; }
.sess-item { padding: 12px 14px; border-radius: var(--radius-sm); margin-bottom: 6px; cursor: pointer; border: 1px solid var(--color-border); }
.sess-item:hover { background: var(--color-brand-light); border-color: var(--color-brand); }
.sess-id { font-size: 12px; font-weight: 500; font-family: var(--font-mono); color: var(--color-text); margin-bottom: 2px; }
.sess-preview { font-size: 12px; color: var(--color-text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
