<template>
  <div class="page-container">
    <div class="page-header">
      <h2>AI 代码评审</h2>
    </div>

    <el-row :gutter="20">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight:600">提交评审</span>
          </template>

          <el-upload
            drag
            multiple
            :auto-upload="false"
            :on-change="handleFileAdd"
            :on-remove="handleFileRemove"
            :file-list="fileList"
            accept=".java,.xml,.json,.yml,.yaml,.txt,.md,.py,.js,.ts,.sql"
          >
            <el-icon :size="40" color="#4a90d9"><UploadFilled /></el-icon>
            <div class="upload-text">拖拽文件到此处，或点击上传</div>
            <template #tip>
              <div class="upload-tip">支持 Java、XML、JSON、SQL 等代码文件</div>
            </template>
          </el-upload>

          <input
            ref="folderInputRef"
            type="file"
            webkitdirectory
            multiple
            style="display:none"
            @change="handleFolderSelect"
          />
          <el-button size="small" style="width:100%;margin-top:8px" @click="folderInputRef?.click()">
            <el-icon><FolderOpened /></el-icon> 上传文件夹
          </el-button>

          <el-divider />

          <el-form label-position="top">
            <el-form-item label="评审维度">
              <el-checkbox-group v-model="checkedDimensions">
                <el-checkbox v-for="dim in reviewDimensions" :key="dim.value" :label="dim.value">
                  {{ dim.label }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              style="width:100%"
              :loading="reviewing"
              :disabled="fileList.length === 0"
              @click="doReview"
            >
              {{ reviewing ? '评审中...' : '开始评审' }}
            </el-button>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card shadow="never" v-loading="reviewing">
          <template #header>
            <div class="result-header">
              <span style="font-weight:600">评审报告</span>
              <template v-if="report">
                <el-tag type="danger" size="small">Critical: {{ report.criticalIssues?.length || 0 }}</el-tag>
                <el-tag type="warning" size="small">Warning: {{ report.warningIssues?.length || 0 }}</el-tag>
                <el-tag type="info" size="small">Info: {{ report.infoIssues?.length || 0 }}</el-tag>
                <el-rate
                  :model-value="report.overallScore || 0"
                  :max="5"
                  disabled
                  show-score
                  size="small"
                />
              </template>
            </div>
          </template>

          <div v-if="!report && !reviewing" class="empty-state">
            <el-icon :size="64" color="#cbd5e1"><Document /></el-icon>
            <p>上传代码文件后点击「开始评审」</p>
          </div>

          <div v-if="report" class="report-content">
            <div v-if="report.criticalIssues?.length" class="issue-section">
              <h4 class="issue-section-title critical">Critical 问题 (必须修复)</h4>
              <div v-for="(issue, i) in report.criticalIssues" :key="'c'+i" class="issue-card critical-border">
                <div class="issue-location">
                  <el-tag type="danger" size="small">{{ issue.severity }}</el-tag>
                  <span>{{ issue.filePath }}:{{ issue.lineNumber }}</span>
                  <el-tag size="small" type="info">{{ issue.dimension }}</el-tag>
                </div>
                <p class="issue-desc">{{ issue.description }}</p>
                <el-alert v-if="issue.impact" title="影响" :description="issue.impact" type="error" :closable="false" />
                <el-alert v-if="issue.suggestion" title="建议" :description="issue.suggestion" type="success" :closable="false" class="mt-8" />
              </div>
            </div>

            <div v-if="report.warningIssues?.length" class="issue-section">
              <h4 class="issue-section-title warning">Warning 问题 (建议修复)</h4>
              <div v-for="(issue, i) in report.warningIssues" :key="'w'+i" class="issue-card warning-border">
                <div class="issue-location">
                  <el-tag type="warning" size="small">{{ issue.severity }}</el-tag>
                  <span>{{ issue.filePath }}:{{ issue.lineNumber }}</span>
                </div>
                <p class="issue-desc">{{ issue.description }}</p>
                <el-alert v-if="issue.suggestion" title="建议" :description="issue.suggestion" type="success" :closable="false" class="mt-8" />
              </div>
            </div>

            <div v-if="report.infoIssues?.length" class="issue-section">
              <h4 class="issue-section-title info">Info 优化建议</h4>
              <div v-for="(issue, i) in report.infoIssues" :key="'i'+i" class="issue-card info-border">
                <div class="issue-location">
                  <el-tag size="small">{{ issue.severity }}</el-tag>
                  <span>{{ issue.filePath }}:{{ issue.lineNumber }}</span>
                </div>
                <p class="issue-desc">{{ issue.description }}</p>
              </div>
            </div>

            <el-alert v-if="report.summary" :title="report.summary" type="success" :closable="false" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { UploadFilled, Document, FolderOpened } from '@element-plus/icons-vue'
import { aiCodeReview } from '@/api'
import { ElMessage } from 'element-plus'

const fileList = ref([])
const reviewing = ref(false)
const report = ref(null)
const checkedDimensions = ref([])
const folderInputRef = ref(null)

const reviewDimensions = [
  { label: '代码质量', value: 'code-quality' },
  { label: '安全性', value: 'security' },
  { label: '可维护性', value: 'maintainability' },
  { label: '架构设计', value: 'architecture' },
  { label: 'Java特定', value: 'java-specific' },
  { label: '数据库', value: 'database' },
  { label: '测试', value: 'testing' },
  { label: '性能', value: 'performance' }
]

function handleFileAdd(file) {
  fileList.value.push(file)
}

function handleFileRemove(file) {
  const idx = fileList.value.indexOf(file)
  if (idx > -1) fileList.value.splice(idx, 1)
}

function handleFolderSelect(e) {
  const files = e.target.files
  if (!files || files.length === 0) return
  for (const f of files) {
    // el-upload 期望的 file 格式：{ name, raw, ... }
    const wrapped = { name: f.webkitRelativePath || f.name, raw: f }
    fileList.value.push(wrapped)
  }
  // 重置 input 以允许重复选择同一文件夹
  e.target.value = ''
}

async function doReview() {
  if (fileList.value.length === 0) return

  reviewing.value = true
  report.value = null

  try {
    const formData = new FormData()
    for (const f of fileList.value) {
      formData.append('files', f.raw)
    }
    if (checkedDimensions.value.length) {
      formData.append('dimensions', checkedDimensions.value.join(','))
    }

    const res = await aiCodeReview.reviewFiles(formData)
    report.value = res
    ElMessage.success('评审完成')
  } catch (e) {
    ElMessage.error(e.message || '评审失败')
  } finally {
    reviewing.value = false
  }
}
</script>

<style scoped>
.result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
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

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: var(--text-secondary);
}
.empty-state p { margin-top: 16px; }

.report-content {
  max-height: 600px;
  overflow-y: auto;
}

.issue-section-title {
  font-size: 14px;
  margin: 16px 0 8px;
}
.issue-section-title.critical { color: #ef4444; }
.issue-section-title.warning { color: #f59e0b; }
.issue-section-title.info { color: #6366f1; }

.issue-card {
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 12px;
  background: #fafbfc;
}
.issue-card.critical-border { border-left: 3px solid #ef4444; }
.issue-card.warning-border { border-left: 3px solid #f59e0b; }
.issue-card.info-border { border-left: 3px solid #6366f1; }

.issue-location {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
}
.issue-desc {
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.mb-16 { margin-bottom: 16px; }
.mt-8 { margin-top: 8px; }
</style>
