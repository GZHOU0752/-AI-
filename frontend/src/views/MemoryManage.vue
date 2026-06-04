<template>
  <div class="page-container">
    <div class="page-header">
      <h2>记忆管理</h2>
      <p>管理短期会话上下文与长期知识记忆</p>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="never" class="card">
          <template #header>
            <div class="head">
              <span class="card-title">短期记忆</span>
            </div>
          </template>

          <div class="tool">
            <el-select v-model="sid" placeholder="选择会话" size="small" class="tool-sel" filterable @change="onSessionChange">
              <el-option v-for="s in sessions" :key="s.sessionId" :label="s.sessionId" :value="s.sessionId">
                <span style="font-family:var(--font-mono);font-size:12px">{{ s.sessionId }}</span>
                <span style="float:right;color:var(--color-text-muted);font-size:11px;margin-left:8px">{{ s.preview?.substring(0,20) || '' }}</span>
              </el-option>
            </el-select>
            <el-button type="primary" size="small" :disabled="!sid" @click="load">加载</el-button>
            <el-button size="small" :disabled="!sid" @click="doSummary">摘要</el-button>
          </div>

          <div v-if="sum" class="sum-box">
            <div class="sum-head">会话摘要</div>
            <p class="sum-text">{{ sum }}</p>
            <span class="sum-close" @click="sum=''">&times;</span>
          </div>

          <div class="msg-list">
            <div v-if="!msgs.length" class="empty">选择会话后点击加载</div>
            <div v-for="(m,i) in msgs" :key="i" :class="['mi', m.role]">
              <div class="mi-head">
                <span class="mi-role">{{ m.role==='user'?'用户':m.role==='assistant'?'AI':'系统' }}</span>
                <span class="mi-time">{{ fmt(m.timestamp) }}</span>
              </div>
              <p class="mi-body">{{ m.content }}</p>
            </div>
          </div>

        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never" class="card">
          <template #header>
            <div class="head">
              <span class="card-title">长期记忆</span>
            </div>
          </template>

          <div class="sec">
            <div class="sec-label">存入新记忆</div>
            <el-input v-model="lc" type="textarea" :rows="3" placeholder="输入需要长期保留的知识..." size="small" />
            <el-select v-model="cat" size="small" class="gap" style="width:100%">
              <el-option v-for="o in cats" :key="o.v" :label="o.l" :value="o.v" />
            </el-select>
            <el-button type="primary" size="small" class="gap" :loading="saving" @click="store" style="width:100%">存入长期记忆</el-button>
          </div>

          <el-divider />

          <div class="sec">
            <div class="sec-label">相似度召回</div>
            <div class="recall-row">
              <el-input v-model="lq" placeholder="输入查询..." size="small" class="recall-inp" />
              <el-button type="primary" size="small" :loading="recalling" @click="recall">搜索</el-button>
            </div>
          </div>

          <div v-if="lr.length" class="recall-list">
            <div v-for="(r,i) in lr" :key="i" class="ri">
              <span class="ri-n">{{ i+1 }}</span>
              <p>{{ r }}</p>
            </div>
          </div>
          <div v-if="!lr.length && recalled" class="empty">未找到相关记忆</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { memoryApi, knowledgeBase } from '@/api'
import { useAuth } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const { state: authState } = useAuth()

const sid = ref(''); const sessions = ref([]); const msgs = ref([]); const sum = ref('')
const lc = ref(''); const cat = ref('general'); const lq = ref(''); const lr = ref([])
const saving = ref(false); const recalling = ref(false); const recalled = ref(false)
const cats = [{l:'技术文档',v:'tech_doc'},{l:'问答记录',v:'qa_history'},{l:'代码审查',v:'code_review'},{l:'项目规范',v:'project_rule'},{l:'其他',v:'general'}]

function fmt(t) { return t ? new Date(t).toLocaleString('zh-CN',{month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'}) : '' }
async function fetchSessions(){try{sessions.value=await knowledgeBase.listSessions(authState.userId)||[]}catch(e){}}
function onSessionChange(){msgs.value=[];sum.value=''}
async function load(){if(!sid.value)return;try{msgs.value=await memoryApi.getHistory(sid.value)||[]}catch(e){ElMessage.error('加载失败')}}
onMounted(()=>{fetchSessions()})
async function doSummary(){try{const r=await memoryApi.generateSummary(sid.value);sum.value=r.summary||r}catch(e){ElMessage.error('失败')}}
async function store(){if(!lc.value.trim()){ElMessage.warning('请输入内容');return};saving.value=true;try{await memoryApi.storeLongTerm({userId:authState.userId,content:lc.value,category:cat.value});ElMessage.success('已存储');lc.value=''}catch(e){ElMessage.error('失败')}finally{saving.value=false}}
async function recall(){if(!lq.value.trim())return;recalling.value=true;recalled.value=false;lr.value=[];try{const r=await memoryApi.recallLongTerm({userId:authState.userId,query:lq.value});lr.value=r.memories||[];recalled.value=true}catch(e){ElMessage.error('失败')}finally{recalling.value=false}}
</script>

<style scoped>
.card-title { font-weight: 600; font-size: 14px; }
.head { display: flex; align-items: center; justify-content: space-between; }
.card { height: calc(100dvh - 190px); display: flex; flex-direction: column; }
.card :deep(.el-card__body) { flex:1; overflow-y:auto; display:flex; flex-direction:column; }

.tool { display: flex; gap: 6px; margin-bottom: 14px; }
.tool-sel { flex: 1; }

.sum-box { position:relative; padding:12px 14px; margin-bottom:12px; background:var(--color-brand-light); border:1px solid #b7ebd0; border-radius:var(--radius-sm); }
.sum-head { font-size:11px; font-weight:600; color:var(--color-brand); text-transform:uppercase; letter-spacing:.04em; margin-bottom:4px; }
.sum-text { font-size:13px; line-height:1.6; color:var(--color-text); }
.sum-close { position:absolute; top:8px; right:10px; border:none; background:none; font-size:16px; color:var(--color-text-muted); cursor:pointer; }

.msg-list { flex:1; overflow-y:auto; margin-bottom:10px; min-height:0; }
.mi { padding:8px 10px; border-radius:6px; margin-bottom:6px; }
.mi.user { background:var(--color-brand-light); }
.mi.assistant { background:var(--color-border-light); }
.mi.system { background:var(--color-warning-bg); }
.mi-head { display:flex; justify-content:space-between; margin-bottom:2px; }
.mi-role { font-size:11px; font-weight:600; color:var(--color-text-secondary); }
.mi-time { font-size:11px; color:var(--color-text-muted); }
.mi-body { font-size:12px; line-height:1.6; }

.sec { margin-bottom:4px; }
.sec-label { font-size:12px; font-weight:500; color:var(--color-text-secondary); margin-bottom:6px; }
.gap { margin-top:8px; }

.recall-row { display:flex; gap:6px; }
.recall-inp { flex:1; }
.recall-list { max-height:180px; overflow-y:auto; margin-top:10px; }
.ri { display:flex; gap:8px; padding:8px 0; border-bottom:1px solid var(--color-border-light); }
.ri-n { font-weight:700; font-size:12px; color:var(--color-brand); flex-shrink:0; width:18px; }
.ri p { font-size:12px; line-height:1.5; }

.empty { text-align:center; padding:48px 0; color:var(--color-text-muted); font-size:13px; }
</style>
