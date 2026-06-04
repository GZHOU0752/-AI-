<template>
  <div class="page-container">
    <div class="page-header">
      <h2>AI 代码评审</h2>
      <p>上传文件或粘贴代码，自动检测潜在问题</p>
    </div>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">提交评审</span>
          </template>

          <!-- Mode switch -->
          <div class="mode-tabs">
            <button :class="['mode-btn',{active:mode==='file'}]" @click="mode='file'">上传文件</button>
            <button :class="['mode-btn',{active:mode==='paste'}]" @click="mode='paste'">粘贴代码</button>
          </div>

          <!-- File mode -->
          <template v-if="mode==='file'">
            <el-upload
              drag multiple :auto-upload="false"
              :on-change="handleFileAdd" :on-remove="handleFileRemove"
              :file-list="fileList"
              accept=".java,.xml,.json,.yml,.yaml,.txt,.md,.py,.js,.ts,.sql"
            >
              <div class="upload-zone">
                <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
                  <path d="M16 6V18M16 6L11 11M16 6L21 11" stroke="#8f959e" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M6 20V24C6 24.5523 6.44772 25 7 25H25C25.5523 25 26 24.5523 26 24V20" stroke="#8f959e" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                <span class="upload-label">拖拽或点击上传文件</span>
                <span class="upload-hint">支持 Java、XML、JSON、SQL 等</span>
              </div>
            </el-upload>
            <input ref="folderRef" type="file" webkitdirectory multiple style="display:none" @change="handleFolder" />
            <el-button size="small" class="folder-btn" @click="folderRef?.click()">选择文件夹</el-button>
          </template>

          <!-- Paste mode -->
          <template v-if="mode==='paste'">
            <el-input v-model="pasteCode" type="textarea" :rows="10" placeholder="在此粘贴代码..." class="paste-area" />
            <el-input v-model="pasteName" placeholder="文件名（可选）" size="small" class="paste-name" />
          </template>

          <el-divider />

          <el-form label-position="top">
            <el-form-item label="评审维度">
              <el-checkbox-group v-model="checkedDimensions" class="dim-grid">
                <el-checkbox v-for="d in dimensions" :key="d.value" :label="d.value">{{ d.label }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-button type="primary" size="large" class="submit-btn" :loading="reviewing" :disabled="!canSubmit" @click="doReview">
              {{ reviewing ? '评审中...' : '开始评审' }}
            </el-button>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card shadow="never" v-loading="reviewing">
          <template #header>
            <div class="report-head">
              <span class="card-title">评审报告</span>
              <div>
                <el-button v-if="report" size="small" @click="exportMd">导出 MD</el-button>
                <el-button size="small" @click="loadHistory();showHistory=true">历史</el-button>
              </div>
              <template v-if="report">
                <div class="report-counts">
                  <span class="count-item" style="color:var(--color-danger)">Critical {{ report.criticalIssues?.length || 0 }}</span>
                  <span class="count-item" style="color:var(--color-warning)">Warning {{ report.warningIssues?.length || 0 }}</span>
                  <span class="count-item" style="color:var(--color-text-secondary)">Info {{ report.infoIssues?.length || 0 }}</span>
                </div>
                <el-rate :model-value="report.overallScore||0" :max="5" disabled show-score size="small" />
              </template>
            </div>
          </template>
          <div v-if="!report && !reviewing" class="empty">上传文件或粘贴代码后点击评审</div>
          <div v-if="report" class="report-body">
            <div v-if="report.criticalIssues?.length" class="block"><div class="block-title" style="color:var(--color-danger)">需修复的问题</div>
              <div v-for="(issue,i) in report.criticalIssues" :key="'c'+i" class="issue"><div class="issue-top"><span class="badge badge-danger">Critical</span><span class="issue-file">{{ issue.filePath }}:{{ issue.lineNumber }}</span><span class="issue-dim">{{ issue.dimension }}</span></div><p class="issue-desc">{{ issue.description }}</p><div v-if="issue.suggestion" class="issue-tip">{{ issue.suggestion }}</div></div></div>
            <div v-if="report.warningIssues?.length" class="block"><div class="block-title" style="color:var(--color-warning)">建议修复的问题</div>
              <div v-for="(issue,i) in report.warningIssues" :key="'w'+i" class="issue"><div class="issue-top"><span class="badge badge-warn">Warning</span><span class="issue-file">{{ issue.filePath }}:{{ issue.lineNumber }}</span></div><p class="issue-desc">{{ issue.description }}</p><div v-if="issue.suggestion" class="issue-tip">{{ issue.suggestion }}</div></div></div>
            <div v-if="report.infoIssues?.length" class="block"><div class="block-title" style="color:var(--color-text-secondary)">优化建议</div>
              <div v-for="(issue,i) in report.infoIssues" :key="'i'+i" class="issue"><div class="issue-top"><span class="badge badge-info">Info</span><span class="issue-file">{{ issue.filePath }}:{{ issue.lineNumber }}</span></div><p class="issue-desc">{{ issue.description }}</p></div></div>
            <div v-if="report.summary" class="summary">{{ report.summary }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-drawer v-model="showHistory" title="评审历史" :size="380" direction="rtl">
      <div v-if="!historyList.length" style="text-align:center;padding:48px 0;color:var(--color-text-muted)">暂无历史记录</div>
      <div v-for="h in historyList" :key="h.id" class="hist-item" @click="loadReport(h)">
        <div class="hist-head">
          <span class="hist-title">{{ h.title }}</span>
          <span class="hist-score">{{ h.score }}/5</span>
        </div>
        <div class="hist-stats">
          <span style="color:var(--color-danger)">C:{{ h.critical }}</span>
          <span style="color:var(--color-warning)">W:{{ h.warning }}</span>
          <span style="color:var(--color-text-secondary)">I:{{ h.info }}</span>
          <span style="margin-left:auto;color:var(--color-text-muted);font-size:11px">{{ h.time }}</span>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { aiCodeReview } from '@/api'
import { useAuth } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const mode = ref('file')
const pasteCode = ref('')
const pasteName = ref('')
const fileList = ref([])
const reviewing = ref(false)
const report = ref(null)
const { state: authState } = useAuth()
const checkedDimensions = ref(['code-quality','security','maintainability','architecture','java-specific','database','testing','performance'])
const folderRef = ref(null)
const showHistory = ref(false)
const historyList = ref([])

const canSubmit = computed(() => mode.value === 'file' ? fileList.value.length > 0 : pasteCode.value.trim().length > 0)

const dimensions = [
  { label: '代码质量', value: 'code-quality' },{ label: '安全性', value: 'security' },
  { label: '可维护性', value: 'maintainability' },{ label: '架构设计', value: 'architecture' },
  { label: 'Java 特定', value: 'java-specific' },{ label: '数据库', value: 'database' },
  { label: '测试', value: 'testing' },{ label: '性能', value: 'performance' }
]

function handleFileAdd(f) { fileList.value.push(f) }
function handleFileRemove(f) { const i=fileList.value.indexOf(f); if(i>-1)fileList.value.splice(i,1) }
function handleFolder(e) { const fs=e.target.files; if(fs?.length) for(const f of fs) fileList.value.push({name:f.webkitRelativePath||f.name,raw:f}); e.target.value='' }

async function doReview() {
  reviewing.value = true; report.value = null
  try {
    if (mode.value === 'paste') {
      report.value = await aiCodeReview.review({
        title: pasteName.value || '粘贴代码评审',
        fileChanges: [{ filePath: pasteName.value || 'pasted-code.java', fileName: pasteName.value || 'pasted-code.java', diffContent: pasteCode.value, changeType: 'MODIFIED' }],
        focusDimensions: checkedDimensions.value.length ? checkedDimensions.value : null
      }, authState.userId)
    } else {
      const fd = new FormData()
      for (const f of fileList.value) fd.append('files', f.raw)
      if (checkedDimensions.value.length) fd.append('dimensions', checkedDimensions.value.join(','))
      fd.append('userId', authState.userId)
      report.value = await aiCodeReview.reviewFiles(fd)
    }
    ElMessage.success('评审完成')
  } catch(e) { ElMessage.error(e.message||'评审失败') }
  finally { reviewing.value = false }
}

function exportMd() {
  if (!report.value) return
  let md = '# 代码评审报告\n\n'
  md += `**评分**: ${report.value.overallScore}/5\n\n`
  if (report.value.summary) md += `**总结**: ${report.value.summary}\n\n`
  const sections = [
    { title: '## Critical 问题', list: report.value.criticalIssues },
    { title: '## Warning 问题', list: report.value.warningIssues },
    { title: '## Info 建议', list: report.value.infoIssues }
  ]
  for (const s of sections) {
    if (!s.list?.length) continue
    md += s.title + '\n\n'
    for (const i of s.list) {
      md += `- **${i.filePath}:${i.lineNumber}** ${i.description}\n`
      if (i.suggestion) md += `  > ${i.suggestion}\n`
    }
    md += '\n'
  }
  const blob = new Blob([md], { type: 'text/markdown' })
  const a = document.createElement('a'); a.href = URL.createObjectURL(blob)
  a.download = `review-report-${Date.now()}.md`; a.click()
  ElMessage.success('报告已下载')
}

async function loadHistory() { try { historyList.value = await aiCodeReview.listHistory(authState.userId) || [] } catch(e){} }

function loadReport(h) {
  try { const r = JSON.parse(h.data); report.value = r } catch(e) { ElMessage.error('加载失败') }
  showHistory.value = false
}

onMounted(() => { loadHistory() })
</script>

<style scoped>
.card-title { font-weight:600;font-size:14px; }
.mode-tabs { display:flex; background:var(--color-border-light); border-radius:8px; padding:3px; margin-bottom:12px; }
.mode-btn { flex:1; padding:7px 0; border:none; background:transparent; font-size:12px; color:var(--color-text-secondary); border-radius:6px; cursor:pointer; transition:all .15s var(--ease); font-family:var(--font-sans); }
.mode-btn.active { background:#fff; color:var(--color-text); box-shadow:0 1px 3px rgba(0,0,0,.06); }
.mode-btn:active { transform:scale(.97); }
.paste-area :deep(textarea) { font-family:var(--font-mono); font-size:12px; line-height:1.5; }
.paste-name { margin-top:8px; }
.upload-zone { display:flex;flex-direction:column;align-items:center;gap:6px;padding:8px 0; }
.upload-label { font-size:13px;color:var(--color-text-secondary); }
.upload-hint { font-size:12px;color:var(--color-text-muted); }
.folder-btn { width:100%;margin-top:8px; }
.dim-grid { display:grid;grid-template-columns:1fr 1fr;gap:2px 0; }
.submit-btn { width:100%;height:42px;font-weight:600; }
.report-head { display:flex;align-items:center;gap:14px;flex-wrap:wrap; }
.report-counts { display:flex;gap:12px; }
.count-item { font-size:12px;font-weight:600; }
.report-body { max-height:540px;overflow-y:auto; }
.block { margin-bottom:18px; }
.block-title { font-size:13px;font-weight:600;margin-bottom:10px; }
.issue { padding:12px 14px;border-radius:var(--radius-sm);background:var(--color-surface);border:1px solid var(--color-border-light);margin-bottom:8px; }
.issue-top { display:flex;align-items:center;gap:8px;margin-bottom:6px;font-size:12px; }
.issue-file { color:var(--color-text-muted);font-family:var(--font-mono);font-size:11px; }
.issue-dim { margin-left:auto;color:var(--color-text-muted);font-size:11px;padding:1px 6px;background:var(--color-border-light);border-radius:4px; }
.issue-desc { font-size:13px;line-height:1.6;margin-bottom:6px; }
.issue-tip { font-size:12px;color:var(--color-brand);background:var(--color-brand-light);padding:8px 10px;border-radius:4px;line-height:1.5; }
.badge { padding:1px 6px;border-radius:4px;font-size:11px;font-weight:600;text-transform:uppercase; }
.badge-danger { background:var(--color-danger-bg);color:var(--color-danger); }
.badge-warn { background:var(--color-warning-bg);color:var(--color-warning); }
.badge-info { background:var(--color-border-light);color:var(--color-text-secondary); }
.summary { margin-top:12px;padding:12px 14px;background:var(--color-success-bg);border-radius:var(--radius-sm);font-size:13px;color:#1a7a3a; }
.empty { text-align:center;padding:80px 0;color:var(--color-text-muted);font-size:14px; }
.hist-item { padding:12px 14px;border-radius:8px;margin-bottom:8px;cursor:pointer;border:1px solid var(--color-border);transition:all .15s; }
.hist-item:hover { background:var(--color-brand-light);border-color:var(--color-brand); }
.hist-head { display:flex;justify-content:space-between;margin-bottom:4px; }
.hist-title { font-size:13px;font-weight:500;overflow:hidden;text-overflow:ellipsis;white-space:nowrap; }
.hist-score { font-size:13px;font-weight:700;color:var(--color-brand); }
.hist-stats { display:flex;gap:10px;font-size:12px; }
</style>
