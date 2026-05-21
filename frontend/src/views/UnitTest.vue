<template>
  <div class="page-container">
    <div class="page-header">
      <h2>单元测试</h2>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="生成测试" name="generate">
        <el-row :gutter="20">
          <el-col :span="10">
            <el-card shadow="never">
              <template #header>
                <span style="font-weight:600">源码文件</span>
              </template>

              <el-upload
                drag
                multiple
                :auto-upload="false"
                :on-change="handleGenFile"
                :on-remove="handleGenFileRemove"
                :file-list="genFileList"
                accept=".java"
              >
                <el-icon :size="40" color="#4a90d9"><UploadFilled /></el-icon>
                <div class="upload-text">上传 Java 源码文件</div>
              </el-upload>

              <input
                ref="genFolderInputRef"
                type="file"
                webkitdirectory
                multiple
                style="display:none"
                @change="handleGenFolderSelect"
              />
              <el-button size="small" style="width:100%;margin-top:8px" @click="genFolderInputRef?.click()">
                <el-icon><FolderOpened /></el-icon> 上传源码文件夹
              </el-button>

              <el-button
                type="primary"
                size="large"
                style="width:100%;margin-top:16px"
                :loading="generating"
                :disabled="genFileList.length === 0"
                @click="doGenerate"
              >
                {{ generating ? '生成中...' : '生成测试代码' }}
              </el-button>
            </el-card>
          </el-col>

          <el-col :span="14">
            <el-card shadow="never" v-loading="generating">
              <template #header>
                <div class="card-flex-header">
                  <span style="font-weight:600">生成的测试代码</span>
                  <el-button v-if="generatedCode" size="small" @click="copyCode">
                    <el-icon><CopyDocument /></el-icon> 复制
                  </el-button>
                </div>
              </template>
              <div v-if="!generatedCode" class="empty-state">
                <el-icon :size="48" color="#cbd5e1"><Notebook /></el-icon>
                <p>上传源码文件后点击「生成测试代码」</p>
              </div>
              <el-input v-else :model-value="generatedCode" type="textarea" :rows="22" readonly class="code-output" />
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="覆盖率分析" name="coverage">
        <el-row :gutter="20">
          <el-col :span="10">
            <el-card shadow="never">
              <template #header>
                <span style="font-weight:600">覆盖率输入</span>
              </template>

              <div style="margin-bottom:16px">
                <div class="file-label">源码文件</div>
                <el-upload
                  drag
                  multiple
                  :auto-upload="false"
                  :on-change="(f) => handleCovFile(f, 'source')"
                  :on-remove="(f) => handleCovFileRemove(f, 'source')"
                  :file-list="sourceFileList"
                  accept=".java"
                >
                  <el-icon :size="32" color="#4a90d9"><UploadFilled /></el-icon>
                  <div class="upload-text">上传源码</div>
                </el-upload>

                <input
                  ref="covSourceFolderRef"
                  type="file"
                  webkitdirectory
                  multiple
                  style="display:none"
                  @change="(e) => handleCovFolderSelect(e, 'source')"
                />
                <el-button size="small" style="width:100%;margin-top:8px" @click="covSourceFolderRef?.click()">
                  <el-icon><FolderOpened /></el-icon> 上传源码文件夹
                </el-button>
              </div>

              <div style="margin-bottom:16px">
                <div class="file-label">测试代码文件（可选）</div>
                <el-upload
                  drag
                  multiple
                  :auto-upload="false"
                  :on-change="(f) => handleCovFile(f, 'test')"
                  :on-remove="(f) => handleCovFileRemove(f, 'test')"
                  :file-list="testFileList"
                  accept=".java"
                >
                  <el-icon :size="32" color="#4a90d9"><UploadFilled /></el-icon>
                  <div class="upload-text">上传已有测试代码</div>
                </el-upload>

                <input
                  ref="covTestFolderRef"
                  type="file"
                  webkitdirectory
                  multiple
                  style="display:none"
                  @change="(e) => handleCovFolderSelect(e, 'test')"
                />
                <el-button size="small" style="width:100%;margin-top:8px" @click="covTestFolderRef?.click()">
                  <el-icon><FolderOpened /></el-icon> 上传测试代码文件夹
                </el-button>
              </div>

              <el-button
                type="primary"
                size="large"
                style="width:100%"
                :loading="analyzing"
                :disabled="sourceFileList.length === 0"
                @click="doAnalyze"
              >
                分析覆盖率
              </el-button>
            </el-card>
          </el-col>

          <el-col :span="14">
            <el-card shadow="never" v-loading="analyzing">
              <template #header>
                <span style="font-weight:600">覆盖率报告</span>
              </template>

              <div v-if="!coverageReport" class="empty-state">
                <el-icon :size="48" color="#cbd5e1"><PieChart /></el-icon>
                <p>上传源码后点击「分析覆盖率」</p>
              </div>

              <div v-if="coverageReport" class="coverage-report">
                <el-row :gutter="16">
                  <el-col :span="6" v-for="metric in metrics" :key="metric.key">
                    <div class="coverage-metric">
                      <el-progress type="dashboard" :percentage="metric.value" :color="metric.color" :stroke-width="8" :width="100">
                        <template #default="{ percentage }">
                          <span class="metric-value">{{ percentage }}%</span>
                        </template>
                      </el-progress>
                      <p class="metric-label">{{ metric.label }}</p>
                      <p class="metric-target">目标: ≥{{ metric.target }}%</p>
                    </div>
                  </el-col>
                </el-row>

                <el-divider />

                <el-result :icon="ratingIcon" :title="coverageReport.overallRating">
                  <template #extra>
                    <el-tag :type="riskTagType" size="large">生产风险: {{ coverageReport.riskLevel }}</el-tag>
                    <el-tag type="warning" size="large" class="ml-8">建议修复时间: {{ coverageReport.suggestedFixTime }}</el-tag>
                  </template>
                </el-result>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { UploadFilled, CopyDocument, Notebook, PieChart, FolderOpened } from '@element-plus/icons-vue'
import { unitTest } from '@/api'
import { ElMessage } from 'element-plus'

const activeTab = ref('generate')

const genFileList = ref([])
const generating = ref(false)
const generatedCode = ref('')
const genFolderInputRef = ref(null)

const sourceFileList = ref([])
const testFileList = ref([])
const analyzing = ref(false)
const coverageReport = ref(null)
const covSourceFolderRef = ref(null)
const covTestFolderRef = ref(null)

const metrics = computed(() => {
  if (!coverageReport.value) return []
  const r = coverageReport.value
  return [
    { key: 'line', label: '行覆盖率', value: +(r.lineCoverage * 100).toFixed(1), target: 80, color: '#22c55e' },
    { key: 'branch', label: '分支覆盖率', value: +(r.branchCoverage * 100).toFixed(1), target: 75, color: '#4a90d9' },
    { key: 'method', label: '方法覆盖率', value: +(r.methodCoverage * 100).toFixed(1), target: 100, color: '#f59e0b' },
    { key: 'exception', label: '异常覆盖率', value: +(r.exceptionCoverage * 100).toFixed(1), target: 100, color: '#ef4444' }
  ]
})

const ratingIcon = computed(() => {
  const r = coverageReport.value?.overallRating
  if (r === '优秀' || r === '良好') return 'success'
  if (r === '待改进') return 'warning'
  return 'error'
})

const riskTagType = computed(() => {
  const r = coverageReport.value?.riskLevel
  if (r === '低') return 'success'
  if (r === '中') return 'warning'
  return 'danger'
})

function handleGenFile(file) {
  genFileList.value.push(file)
}

function handleGenFileRemove(file) {
  const idx = genFileList.value.indexOf(file)
  if (idx > -1) genFileList.value.splice(idx, 1)
}

function handleGenFolderSelect(e) {
  const files = e.target.files
  if (!files || files.length === 0) return
  for (const f of files) {
    genFileList.value.push({ name: f.webkitRelativePath || f.name, raw: f })
  }
  e.target.value = ''
}

function handleCovFile(file, type) {
  if (type === 'source') {
    sourceFileList.value.push(file)
  } else {
    testFileList.value.push(file)
  }
}

function handleCovFileRemove(file, type) {
  const list = type === 'source' ? sourceFileList.value : testFileList.value
  const idx = list.indexOf(file)
  if (idx > -1) list.splice(idx, 1)
}

function handleCovFolderSelect(e, type) {
  const files = e.target.files
  if (!files || files.length === 0) return
  for (const f of files) {
    const wrapped = { name: f.webkitRelativePath || f.name, raw: f }
    if (type === 'source') {
      sourceFileList.value.push(wrapped)
    } else {
      testFileList.value.push(wrapped)
    }
  }
  e.target.value = ''
}

async function doGenerate() {
  if (genFileList.value.length === 0) return
  generating.value = true
  generatedCode.value = ''
  try {
    const codes = []
    for (const f of genFileList.value) {
      const fd = new FormData()
      fd.append('file', f.raw)
      const res = await unitTest.generateFromFile(fd)
      codes.push(`// ====== ${f.name} ======\n${res.testCode || ''}`)
    }
    generatedCode.value = codes.join('\n\n')
    ElMessage.success(`已生成 ${genFileList.value.length} 个文件的测试代码`)
  } catch (e) {
    ElMessage.error('生成失败: ' + e.message)
  } finally {
    generating.value = false
  }
}

function copyCode() {
  navigator.clipboard.writeText(generatedCode.value)
  ElMessage.success('已复制')
}

async function doAnalyze() {
  if (sourceFileList.value.length === 0) return
  analyzing.value = true
  coverageReport.value = null
  try {
    const sourceFile = sourceFileList.value[0].raw
    const testFile = testFileList.value.length > 0 ? testFileList.value[0].raw : null
    const fd = new FormData()
    fd.append('sourceFile', sourceFile)
    if (testFile) fd.append('testFile', testFile)
    const res = await unitTest.analyzeFromFiles(fd)
    coverageReport.value = res
  } catch (e) {
    ElMessage.error('分析失败: ' + e.message)
  } finally {
    analyzing.value = false
  }
}
</script>

<style scoped>
.card-flex-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.upload-text {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}
.file-label {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: var(--text-secondary);
}
.empty-state p { margin-top: 12px; }

.code-output :deep(textarea) {
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.5;
}

.coverage-metric { text-align: center; }
.metric-label { font-size: 13px; font-weight: 500; margin-top: 8px; }
.metric-target { font-size: 11px; color: var(--text-secondary); margin-top: 2px; }
.ml-8 { margin-left: 8px; }
</style>
