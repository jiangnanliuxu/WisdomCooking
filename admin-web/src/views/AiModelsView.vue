<script setup lang="ts">
import { Cpu } from '@element-plus/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAiModel,
  enableAiModel,
  listAiModels,
  saveAiPrompt,
  testAiModel,
  updateAiModel,
} from '@/api/ai'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import type { AiModel, AiModelForm } from '@/types/cook'
import { safeJson } from '@/utils/format'

const loading = ref(false)
const models = ref<AiModel[]>([])
const page = reactive({ page: 1, pageSize: 20, total: 0 })
const filters = reactive({ modelType: 'chat', status: '', keyword: '' })
const activeModelId = ref<number>()

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number>()
const form = reactive<AiModelForm>({
  name: '',
  modelType: 'chat',
  provider: 'aliyun-qwen',
  modelCode: '',
  encryptedApiKey: '',
  apiBaseUrl: '',
  configJson: JSON.stringify({ temperature: 0.7, max_tokens: 1024 }, null, 2),
  systemPrompt: '',
  isDefault: false,
  status: 'enabled',
})

const promptVisible = ref(false)
const promptForm = reactive({
  id: 0,
  systemPrompt: '',
  fewShotExamplesJson: '[]',
})

const configDraft = reactive({
  temperature: 0.7,
  maxTokens: 2048,
  topP: 0.9,
  repeatPenalty: 1.1,
  systemPrompt: '',
})

const selectedModel = computed(() => models.value.find(item => item.id === activeModelId.value) || models.value[0])

function resetForm() {
  Object.assign(form, {
    name: '',
    modelType: filters.modelType || 'chat',
    provider: 'aliyun-qwen',
    modelCode: '',
    encryptedApiKey: '',
    apiBaseUrl: '',
    configJson: JSON.stringify({ temperature: 0.7, max_tokens: 1024 }, null, 2),
    systemPrompt: '',
    isDefault: false,
    status: 'enabled',
  })
  editingId.value = undefined
}

function toNumber(value: unknown, fallback: number) {
  const num = Number(value)
  return Number.isFinite(num) ? num : fallback
}

function syncConfigDraft(model?: AiModel) {
  const config = safeJson(model?.configJson)
  configDraft.temperature = toNumber(config.temperature, 0.7)
  configDraft.maxTokens = toNumber(config.max_tokens ?? config.maxTokens, 2048)
  configDraft.topP = toNumber(config.top_p ?? config.topP, 0.9)
  configDraft.repeatPenalty = toNumber(config.repetition_penalty ?? config.repeatPenalty ?? config.presence_penalty, 1.1)
  configDraft.systemPrompt = model?.systemPrompt || ''
}

function selectModel(model: AiModel) {
  activeModelId.value = model.id
  syncConfigDraft(model)
}

function getProviderTone(provider?: string) {
  if (provider?.includes('aliyun') || provider?.includes('qwen')) return 'provider-blue'
  if (provider?.includes('ernie') || provider?.includes('baidu')) return 'provider-purple'
  if (provider?.includes('glm') || provider?.includes('zhipu')) return 'provider-green'
  if (provider?.includes('deepseek')) return 'provider-orange'
  return 'provider-default'
}

function modelTypeLabel(type?: string) {
  return type === 'vision' ? '视觉模型' : '对话模型'
}

async function loadData() {
  loading.value = true
  try {
    const response = await listAiModels({
      ...filters,
      page: page.page,
      pageSize: page.pageSize,
    })
    models.value = response.data?.items || []
    page.total = response.data?.total || 0

    const nextActiveId = models.value.find(item => item.id === activeModelId.value)?.id
      || models.value.find(item => item.isDefault)?.id
      || models.value[0]?.id

    activeModelId.value = nextActiveId
    syncConfigDraft(selectedModel.value)
  }
  finally {
    loading.value = false
  }
}

function handleCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(model: AiModel) {
  dialogMode.value = 'edit'
  editingId.value = model.id
  Object.assign(form, {
    name: model.name,
    modelType: model.modelType,
    provider: model.provider,
    modelCode: model.modelCode,
    encryptedApiKey: '',
    apiBaseUrl: model.apiBaseUrl || '',
    configJson: model.configJson || '{}',
    systemPrompt: model.systemPrompt || '',
    isDefault: model.isDefault,
    status: model.status,
  })
  dialogVisible.value = true
}

async function submitForm() {
  const payload = { ...form }
  if (dialogMode.value === 'create') {
    await createAiModel(payload)
    ElMessage.success('模型已创建')
  }
  else if (editingId.value) {
    await updateAiModel(editingId.value, payload)
    ElMessage.success('模型已更新')
  }
  dialogVisible.value = false
  await loadData()
}

async function handleEnable(model: AiModel) {
  await ElMessageBox.confirm(`确认将 ${model.name} 设为默认${model.modelType === 'chat' ? '对话' : '视觉'}模型？`, '启用模型', {
    type: 'warning',
  })
  await enableAiModel(model.id)
  ElMessage.success('默认模型已切换')
  await loadData()
}

async function handleTest(model: AiModel) {
  const response = await testAiModel(model.id)
  const data = response.data as { status?: string; latencyMs?: number; message?: string } | undefined
  ElMessage.success(`测试完成：${data?.status || 'unknown'} / ${data?.latencyMs || 0}ms`)
  await loadData()
}

function openPrompt(model: AiModel) {
  promptForm.id = model.id
  promptForm.systemPrompt = model.systemPrompt || ''
  promptForm.fewShotExamplesJson = JSON.stringify(safeJson(model.configJson).fewShotExamples || [], null, 2)
  promptVisible.value = true
}

async function submitPrompt() {
  await saveAiPrompt(promptForm.id, {
    systemPrompt: promptForm.systemPrompt,
    fewShotExamplesJson: promptForm.fewShotExamplesJson,
  })
  ElMessage.success('Prompt 已保存')
  promptVisible.value = false
  await loadData()
}

async function saveActiveConfig() {
  const model = selectedModel.value
  if (!model) return

  const config = safeJson(model.configJson)
  const payload: AiModelForm = {
    name: model.name,
    modelType: model.modelType,
    provider: model.provider,
    modelCode: model.modelCode,
    apiBaseUrl: model.apiBaseUrl || '',
    configJson: JSON.stringify({
      ...config,
      temperature: configDraft.temperature,
      max_tokens: configDraft.maxTokens,
      top_p: configDraft.topP,
      repetition_penalty: configDraft.repeatPenalty,
    }, null, 2),
    systemPrompt: configDraft.systemPrompt,
    isDefault: model.isDefault,
    status: model.status,
  }

  await updateAiModel(model.id, payload)
  ElMessage.success('当前模型配置已保存')
  await loadData()
}

function resetActiveConfig() {
  syncConfigDraft(selectedModel.value)
}

onMounted(loadData)
</script>

<template>
  <PageHeader title="AI 模型配置" description="统一管理对话与视觉模型，默认模型切换后会直接影响管理端和用户端调用。">
    <template #actions>
      <el-button type="primary" @click="handleCreate">
        添加模型
      </el-button>
    </template>
  </PageHeader>

  <div class="page-toolbar">
    <div class="page-toolbar__filters">
      <el-select v-model="filters.modelType" style="width: 160px;" @change="loadData">
        <el-option label="对话模型" value="chat" />
        <el-option label="视觉模型" value="vision" />
      </el-select>
      <el-select v-model="filters.status" clearable style="width: 160px;" placeholder="状态" @change="loadData">
        <el-option label="已启用" value="enabled" />
        <el-option label="已停用" value="disabled" />
      </el-select>
      <el-input v-model="filters.keyword" clearable style="width: 240px;" placeholder="搜索模型名称或标识" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">
        查询
      </el-button>
    </div>
  </div>

  <div v-loading="loading">
    <div v-if="models.length" class="model-cards">
      <article
        v-for="item in models"
        :key="item.id"
        class="model-card"
        :class="{ 'model-card--active': item.id === selectedModel?.id }"
        @click="selectModel(item)"
      >
        <div class="model-card__header">
          <div class="model-card__title">
            <div class="model-card__icon" :class="getProviderTone(item.provider)">
              <el-icon size="22">
                <Cpu />
              </el-icon>
            </div>
            <div>
              <div class="model-card__name">
                {{ item.name }}
              </div>
              <div class="model-card__provider">
                {{ item.provider }} · {{ item.modelCode }}
              </div>
            </div>
          </div>
          <StatusTag :value="item.isDefault ? 'enabled' : item.status" />
        </div>

        <div class="model-card__meta">
          <div class="model-card__meta-item">
            <span>模型类型</span>
            <strong>{{ modelTypeLabel(item.modelType) }}</strong>
          </div>
          <div class="model-card__meta-item">
            <span>API 地址</span>
            <strong class="mono">{{ item.apiBaseUrl || '使用默认地址' }}</strong>
          </div>
          <div class="model-card__meta-item">
            <span>最近测试</span>
            <strong>{{ item.lastTestStatus || '未测试' }}</strong>
          </div>
          <div class="model-card__meta-item">
            <span>响应耗时</span>
            <strong>{{ item.lastTestLatencyMs || 0 }} ms</strong>
          </div>
        </div>

        <div class="model-card__message">
          {{ item.lastTestMessage || '当前暂无测试结果，建议在切换前执行连通测试。' }}
        </div>

        <div class="model-card__actions" @click.stop>
          <el-button v-if="!item.isDefault" type="primary" plain @click="handleEnable(item)">
            设为默认
          </el-button>
          <el-button plain @click="handleEdit(item)">
            编辑配置
          </el-button>
          <el-button plain @click="openPrompt(item)">
            Prompt 配置
          </el-button>
          <el-button plain @click="handleTest(item)">
            连通测试
          </el-button>
        </div>
      </article>
    </div>

    <el-empty v-else description="暂无模型配置数据" />

    <section v-if="selectedModel" class="config-section">
      <div class="config-section__header">
        <div>
          <h3>当前模型参数配置（{{ selectedModel.name }}）</h3>
          <p>{{ selectedModel.provider }} · {{ selectedModel.modelCode }}</p>
        </div>
        <div class="config-section__actions">
          <el-button plain @click="openPrompt(selectedModel)">
            高级 Prompt
          </el-button>
          <el-button plain @click="handleEdit(selectedModel)">
            完整编辑
          </el-button>
        </div>
      </div>

      <el-form label-position="top">
        <div class="config-form-grid">
          <el-form-item label="温度 (Temperature)">
            <el-input v-model.number="configDraft.temperature" type="number" />
            <div class="field-hint">控制回答的随机性，值越大回答越多样化（0-2）</div>
          </el-form-item>

          <el-form-item label="最大 Token 数">
            <el-input v-model.number="configDraft.maxTokens" type="number" />
            <div class="field-hint">单次响应可返回的最大 Token 数量</div>
          </el-form-item>

          <el-form-item label="Top-P">
            <el-input v-model.number="configDraft.topP" type="number" />
            <div class="field-hint">控制候选词范围，越小越保守</div>
          </el-form-item>

          <el-form-item label="重复惩罚">
            <el-input v-model.number="configDraft.repeatPenalty" type="number" />
            <div class="field-hint">值越大越能抑制重复输出</div>
          </el-form-item>
        </div>

        <el-form-item class="config-form-full" label="系统提示词 (System Prompt)">
          <el-input v-model="configDraft.systemPrompt" type="textarea" :rows="7" />
        </el-form-item>
      </el-form>

      <div class="config-section__footer">
        <el-button type="primary" @click="saveActiveConfig">
          保存当前配置
        </el-button>
        <el-button @click="resetActiveConfig">
          重置
        </el-button>
      </div>
    </section>

    <div class="admin-pagination">
      <el-pagination
        v-model:current-page="page.page"
        v-model:page-size="page.pageSize"
        background
        layout="total, prev, pager, next"
        :total="page.total"
        @current-change="loadData"
      />
    </div>
  </div>

  <el-dialog v-model="dialogVisible" class="admin-dialog" :title="dialogMode === 'create' ? '新增模型' : '编辑模型'" width="720px">
    <el-form label-position="top">
      <div class="admin-form-grid">
        <el-form-item label="模型名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="模型类型">
          <el-select v-model="form.modelType">
            <el-option label="对话模型" value="chat" />
            <el-option label="视觉模型" value="vision" />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商"><el-input v-model="form.provider" /></el-form-item>
        <el-form-item label="模型标识"><el-input v-model="form.modelCode" /></el-form-item>
        <el-form-item label="API Key"><el-input v-model="form.encryptedApiKey" show-password placeholder="编辑时留空表示保持原值" /></el-form-item>
        <el-form-item label="API Base URL"><el-input v-model="form.apiBaseUrl" /></el-form-item>
      </div>
      <el-form-item class="admin-form-full" label="模型参数 JSON"><el-input v-model="form.configJson" type="textarea" :rows="6" /></el-form-item>
      <el-form-item class="admin-form-full" label="系统提示词"><el-input v-model="form.systemPrompt" type="textarea" :rows="5" /></el-form-item>
      <div class="dialog-switches">
        <el-checkbox v-model="form.isDefault">
          设为默认模型
        </el-checkbox>
        <el-switch v-model="form.status" active-value="enabled" inactive-value="disabled" />
      </div>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">
        取消
      </el-button>
      <el-button type="primary" @click="submitForm">
        保存
      </el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="promptVisible" class="admin-dialog" title="模型 Prompt 配置" width="720px">
    <el-form label-position="top">
      <el-form-item label="系统提示词"><el-input v-model="promptForm.systemPrompt" type="textarea" :rows="6" /></el-form-item>
      <el-form-item label="Few-shot 预设 JSON"><el-input v-model="promptForm.fewShotExamplesJson" type="textarea" :rows="8" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="promptVisible = false">
        取消
      </el-button>
      <el-button type="primary" @click="submitPrompt">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.model-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.model-card,
.config-section {
  background: #fff;
  border-radius: 12px;
  box-shadow: var(--admin-card-shadow);
}

.model-card {
  padding: 24px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
}

.model-card:hover {
  border-color: rgba(255, 107, 53, 0.16);
  transform: translateY(-1px);
}

.model-card--active {
  border-color: var(--admin-primary);
}

.model-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.model-card__title {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.model-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.provider-blue {
  color: #1890ff;
  background: #e6f7ff;
}

.provider-purple {
  color: #722ed1;
  background: #f9f0ff;
}

.provider-green {
  color: #52c41a;
  background: #f6ffed;
}

.provider-orange {
  color: #fa8c16;
  background: #fff7e6;
}

.provider-default {
  color: #5b6b7e;
  background: #f5f7fa;
}

.model-card__name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.model-card__provider {
  margin-top: 4px;
  font-size: 13px;
  color: #999;
  line-height: 1.5;
  word-break: break-all;
}

.model-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.model-card__meta-item {
  display: grid;
  gap: 4px;
}

.model-card__meta-item span {
  font-size: 12px;
  color: #999;
}

.model-card__meta-item strong {
  font-size: 13px;
  color: #333;
  line-height: 1.6;
  word-break: break-all;
}

.model-card__message {
  min-height: 42px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #fafafa;
  color: #666;
  font-size: 13px;
  line-height: 1.7;
}

.model-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.config-section {
  padding: 24px;
}

.config-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.config-section__header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.config-section__header p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #999;
}

.config-section__actions,
.config-section__footer,
.dialog-switches {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.config-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.config-form-full {
  margin-top: 4px;
}

.field-hint {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #999;
}

.config-section__footer {
  margin-top: 8px;
}
</style>
