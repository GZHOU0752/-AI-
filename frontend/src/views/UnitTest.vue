<template>
  <div class="page-container">
    <div class="page-header">
      <h2>单元测试</h2>
      <p>生成 JUnit 5 + Mockito 测试代码，分析覆盖率</p>
    </div>

    <el-tabs v-model="tab" type="border-card">
      <!-- Generate tests -->
      <el-tab-pane label="生成测试" name="gen">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card shadow="never">
              <template #header><span class="card-title">源码输入</span></template>
              <div class="mode-tabs">
                <button :class="['mode-btn',{active:gMode==='file'}]" @click="gMode='file'">上传文件</button>
                <button :class="['mode-btn',{active:gMode==='paste'}]" @click="gMode='paste'">粘贴代码</button>
              </div>
              <template v-if="gMode==='file'">
                <el-upload drag multiple :auto-upload="false" :on-change="onGenAdd" :on-remove="onGenDel" :file-list="genFiles" accept=".java">
                  <div class="zone"><svg width="28" height="28" viewBox="0 0 28 28" fill="none"><rect x="4" y="5" width="20" height="18" rx="3" stroke="#8f959e" stroke-width="1.4"/><path d="M9 14L13 10L17 14" stroke="#8f959e" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/><path d="M13 10V19" stroke="#8f959e" stroke-width="1.2" stroke-linecap="round"/></svg><span class="zone-label">上传 Java 源码</span></div>
                </el-upload>
                <input ref="gfRef" type="file" webkitdirectory multiple style="display:none" @change="onGenFolder" />
                <el-button size="small" class="aux-btn" @click="gfRef?.click()">选择文件夹</el-button>
              </template>
              <template v-if="gMode==='paste'">
                <el-input v-model="gCode" type="textarea" :rows="8" placeholder="粘贴 Java 源码..." class="code-area" />
                <el-input v-model="gName" placeholder="类名（可选）" size="small" class="mt-sm" />
              </template>
              <el-button type="primary" size="large" class="act-btn" :loading="gen" :disabled="!gReady" @click="doGen">{{ gen?'生成中...':'生成测试代码' }}</el-button>
            </el-card>
          </el-col>
          <el-col :span="16">
            <el-card shadow="never" v-loading="gen">
              <template #header><div class="out-head"><span class="card-title">输出</span><el-button v-if="out" size="small" @click="copyG">复制</el-button></div></template>
              <div v-if="!out" class="empty">上传文件或粘贴代码后点击生成</div>
              <el-input v-else :model-value="out" type="textarea" :rows="19" readonly class="code-out" />
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- Coverage -->
      <el-tab-pane label="覆盖率分析" name="cov">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card shadow="never">
              <template #header><span class="card-title">输入</span></template>
              <div class="mode-tabs">
                <button :class="['mode-btn',{active:cMode==='file'}]" @click="cMode='file'">上传文件</button>
                <button :class="['mode-btn',{active:cMode==='paste'}]" @click="cMode='paste'">粘贴代码</button>
              </div>
              <template v-if="cMode==='file'">
                <div class="fld"><div class="fld-label">源码文件</div>
                  <el-upload drag multiple :auto-upload="false" :on-change="f=>onCovAdd(f,'src')" :on-remove="f=>onCovDel(f,'src')" :file-list="srcFiles" accept=".java">
                    <div class="zone-sm"><svg width="20" height="20" viewBox="0 0 20 20" fill="none"><rect x="2" y="3" width="16" height="14" rx="2" stroke="#8f959e" stroke-width="1.2"/><path d="M6 10L9 7L12 10" stroke="#8f959e" stroke-width="1.1" stroke-linecap="round"/><path d="M9 7V14" stroke="#8f959e" stroke-width="1.1" stroke-linecap="round"/></svg><span class="zone-label-sm">上传源码</span></div>
                  </el-upload>
                  <input ref="csfRef" type="file" webkitdirectory multiple style="display:none" @change="e=>onCovFolder(e,'src')" />
                  <el-button size="small" class="aux-btn" @click="csfRef?.click()">选择文件夹</el-button>
                </div>
                <div class="fld"><div class="fld-label">测试代码（可选）</div>
                  <el-upload drag multiple :auto-upload="false" :on-change="f=>onCovAdd(f,'tst')" :on-remove="f=>onCovDel(f,'tst')" :file-list="tstFiles" accept=".java">
                    <div class="zone-sm"><svg width="20" height="20" viewBox="0 0 20 20" fill="none"><rect x="2" y="3" width="16" height="14" rx="2" stroke="#8f959e" stroke-width="1.2"/><path d="M6 10L9 7L12 10" stroke="#8f959e" stroke-width="1.1" stroke-linecap="round"/><path d="M9 7V14" stroke="#8f959e" stroke-width="1.1" stroke-linecap="round"/></svg><span class="zone-label-sm">上传测试代码</span></div>
                  </el-upload>
                  <input ref="ctfRef" type="file" webkitdirectory multiple style="display:none" @change="e=>onCovFolder(e,'tst')" />
                  <el-button size="small" class="aux-btn" @click="ctfRef?.click()">选择文件夹</el-button>
                </div>
              </template>
              <template v-if="cMode==='paste'">
                <div class="fld"><div class="fld-label">源码</div><el-input v-model="cSrcCode" type="textarea" :rows="6" placeholder="粘贴源码..." class="code-area" /></div>
                <div class="fld"><div class="fld-label">测试代码（可选）</div><el-input v-model="cTstCode" type="textarea" :rows="4" placeholder="粘贴已有测试代码..." class="code-area" /></div>
                <el-input v-model="cName" placeholder="类名（可选）" size="small" />
              </template>
              <el-button type="primary" size="large" class="act-btn" :loading="ana" :disabled="!cReady" @click="doAna">分析覆盖率</el-button>
            </el-card>
          </el-col>
          <el-col :span="16">
            <el-card shadow="never" v-loading="ana">
              <template #header><span class="card-title">报告</span></template>
              <div v-if="!cov" class="empty">上传文件或粘贴代码后点击分析</div>
              <div v-if="cov" class="cov-dash">
                <div class="cov-row"><div v-for="m in metrics" :key="m.key" class="cov-cell"><el-progress type="dashboard" :percentage="m.val" :color="m.color" :stroke-width="6" :width="80"><template #default="{percentage}"><span class="cov-pct">{{percentage}}%</span></template></el-progress><p class="cov-label">{{ m.label }}</p></div></div>
                <el-divider /><div class="cov-rating"><span class="cov-grade" :class="'g-'+cov.overallRating">{{ cov.overallRating }}</span><div class="cov-tags"><span class="rtag" :class="'r-'+cov.riskLevel">风险: {{ cov.riskLevel }}</span><span class="rtag r-neutral">修复时间: {{ cov.suggestedFixTime }}</span></div></div>
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
import { unitTest } from '@/api'
import { ElMessage } from 'element-plus'

const tab = ref('gen')

// Generate
const gMode = ref('file'); const gCode = ref(''); const gName = ref('')
const genFiles = ref([]); const gen = ref(false); const out = ref(''); const gfRef = ref(null)
const gReady = computed(() => gMode.value==='file' ? genFiles.value.length>0 : gCode.value.trim().length>0)

// Coverage
const cMode = ref('file'); const cSrcCode = ref(''); const cTstCode = ref(''); const cName = ref('')
const srcFiles = ref([]); const tstFiles = ref([]); const ana = ref(false); const cov = ref(null)
const csfRef = ref(null); const ctfRef = ref(null)
const cReady = computed(() => cMode.value==='file' ? srcFiles.value.length>0 : cSrcCode.value.trim().length>0)

const metrics = computed(() => !cov.value ? [] : [
  { key:'line',label:'行覆盖率',val:+(cov.value.lineCoverage*100).toFixed(1),color:'#00b96b'},
  { key:'branch',label:'分支覆盖率',val:+(cov.value.branchCoverage*100).toFixed(1),color:'#34c759'},
  { key:'method',label:'方法覆盖率',val:+(cov.value.methodCoverage*100).toFixed(1),color:'#ff9500'},
  { key:'exception',label:'异常覆盖率',val:+(cov.value.exceptionCoverage*100).toFixed(1),color:'#ff3b30'}
])

// Gen handlers
function onGenAdd(f){genFiles.value.push(f)}; function onGenDel(f){const i=genFiles.value.indexOf(f);if(i>-1)genFiles.value.splice(i,1)}
function onGenFolder(e){const fs=e.target.files;if(fs?.length)for(const f of fs)genFiles.value.push({name:f.webkitRelativePath||f.name,raw:f});e.target.value=''}
// Cov handlers
function onCovAdd(f,t){(t==='src'?srcFiles:tstFiles).value.push(f)}
function onCovDel(f,t){const l=(t==='src'?srcFiles:tstFiles).value;const i=l.indexOf(f);if(i>-1)l.splice(i,1)}
function onCovFolder(e,t){const fs=e.target.files;if(fs?.length)for(const f of fs)(t==='src'?srcFiles:tstFiles).value.push({name:f.webkitRelativePath||f.name,raw:f});e.target.value=''}

async function doGen(){
  gen.value=true;out.value=''
  try{
    if(gMode.value==='paste'){const r=await unitTest.generate({sourceCode:gCode.value,targetClassName:gName.value||'PastedCode'});out.value=r.testCode||''}
    else{const cs=[];for(const f of genFiles.value){const fd=new FormData();fd.append('file',f.raw);const r=await unitTest.generateFromFile(fd);cs.push('// ==== '+f.name+' ====\n'+(r.testCode||''))};out.value=cs.join('\n\n')}
    ElMessage.success('完成')
  }catch(e){ElMessage.error(e.message)}finally{gen.value=false}
}
function copyG(){navigator.clipboard.writeText(out.value);ElMessage.success('已复制')}

async function doAna(){
  ana.value=true;cov.value=null
  try{
    if(cMode.value==='paste'){cov.value=await unitTest.analyzeCoverage({className:cName.value||'PastedCode',sourceCode:cSrcCode.value,testCode:cTstCode.value})}
    else{const fd=new FormData();fd.append('sourceFile',srcFiles.value[0].raw);if(tstFiles.value.length)fd.append('testFile',tstFiles.value[0].raw);cov.value=await unitTest.analyzeFromFiles(fd)}
  }catch(e){ElMessage.error(e.message)}finally{ana.value=false}
}
</script>

<style scoped>
.card-title { font-weight:600;font-size:14px; }
.out-head { display:flex;align-items:center;justify-content:space-between; }
.mode-tabs { display:flex;background:var(--color-border-light);border-radius:8px;padding:3px;margin-bottom:10px; }
.mode-btn { flex:1;padding:6px 0;border:none;background:transparent;font-size:12px;color:var(--color-text-secondary);border-radius:6px;cursor:pointer;transition:all .15s var(--ease);font-family:var(--font-sans); }
.mode-btn.active { background:#fff;color:var(--color-text);box-shadow:0 1px 3px rgba(0,0,0,.06); }
.code-area :deep(textarea) { font-family:var(--font-mono);font-size:12px;line-height:1.5; }
.mt-sm { margin-top:8px; }
.zone { display:flex;flex-direction:column;align-items:center;gap:4px;padding:8px 0; }
.zone-label { font-size:13px;color:var(--color-text-secondary); }
.zone-sm { display:flex;flex-direction:column;align-items:center;gap:2px;padding:12px 0; }
.zone-label-sm { font-size:12px;color:var(--color-text-muted); }
.aux-btn { width:100%;margin-top:6px; }
.act-btn { width:100%;margin-top:14px;height:40px;font-weight:600; }
.fld { margin-bottom:14px; }
.fld-label { font-size:12px;font-weight:500;color:var(--color-text-secondary);margin-bottom:6px; }
.code-out :deep(textarea) { font-family:var(--font-mono);font-size:12px;line-height:1.6; }
.empty { text-align:center;padding:80px 0;color:var(--color-text-muted);font-size:14px; }
.cov-dash { padding:4px 0; }
.cov-row { display:flex;justify-content:space-around; }
.cov-cell { text-align:center; }
.cov-pct { font-size:14px;font-weight:700; }
.cov-label { font-size:11px;color:var(--color-text-secondary);margin-top:4px; }
.cov-rating { text-align:center;margin-top:4px; }
.cov-grade { font-size:28px;font-weight:700; }
.g-优秀{color:#34c759}.g-良好{color:#00b96b}.g-待改进{color:#ff9500}
.rtag { font-size:11px;padding:4px 10px;border-radius:4px;font-weight:500; }
.r-低{background:var(--color-success-bg);color:#1a7a3a}.r-中{background:var(--color-warning-bg);color:#92400e}.r-高{background:var(--color-danger-bg);color:#991b1b}.r-neutral{background:var(--color-border-light);color:var(--color-text-secondary)}
.cov-tags { margin-top:10px;display:flex;gap:8px;justify-content:center; }
</style>
