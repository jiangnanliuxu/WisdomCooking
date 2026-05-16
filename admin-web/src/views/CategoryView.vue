<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createCategory, deleteCategory, listAdminCategories, updateCategory } from '@/api/category'
import PageHeader from '@/components/PageHeader.vue'
import type { Category, CategoryGroup } from '@/types/cook'

const ICON_OPTIONS = ['🌶️', '🍗', '🧁', '🥗', '🔥', '🍜', '🍰', '🍲', '🥘', '🍣', '🥩', '🍱', '🥟', '🍢', '🥣', '🥪']
const COLOR_OPTIONS = ['#fff1f0', '#fff7e6', '#f9f0ff', '#f6ffed', '#e6f7ff', '#e6fffb', '#fff0f6', '#fffbe6']

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const groups = ref<CategoryGroup[]>([])
const editingId = ref<number>()

const form = reactive({
  name: '',
  icon: ICON_OPTIONS[0],
  color: COLOR_OPTIONS[0],
  description: '',
  sortNo: 0,
  status: 'enabled',
})

const flatCategories = computed(() =>
  groups.value.flatMap(group =>
    group.children.map(item => ({
      ...item,
      groupCode: item.groupCode || group.code,
      groupName: group.name,
    })),
  ),
)

const totalCategories = computed(() => flatCategories.value.length)
const enabledCategories = computed(() => flatCategories.value.filter(item => item.status !== 'disabled').length)
const lockedCategories = computed(() => flatCategories.value.filter(item => item.readonly).length)
const totalRecipes = computed(() => flatCategories.value.reduce((sum, item) => sum + Number(item.recipeCount || 0), 0))

function statusLabel(status?: string) {
  return status === 'disabled' ? '停用' : '启用'
}

function resetForm() {
  editingId.value = undefined
  form.name = ''
  form.icon = ICON_OPTIONS[0]
  form.color = COLOR_OPTIONS[0]
  form.description = ''
  form.sortNo = 0
  form.status = 'enabled'
}

async function loadData() {
  loading.value = true
  try {
    const response = await listAdminCategories()
    groups.value = response.data || []
  }
  finally {
    loading.value = false
  }
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(item: Category) {
  if (item.readonly) {
    ElMessage.warning('社区广场预选菜系不允许修改')
    return
  }
  editingId.value = item.id
  form.name = item.name
  form.icon = item.icon || ICON_OPTIONS[0]
  form.color = item.color || COLOR_OPTIONS[0]
  form.description = item.description || ''
  form.sortNo = Number(item.sortNo || 0)
  form.status = item.status || 'enabled'
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  if (!form.icon) {
    ElMessage.warning('请选择分类图标')
    return
  }
  if (!form.color) {
    ElMessage.warning('请选择图标颜色')
    return
  }

  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      icon: form.icon,
      color: form.color,
      description: form.description.trim(),
      sortNo: Number(form.sortNo || 0),
      status: form.status,
    }

    if (editingId.value) {
      await updateCategory(editingId.value, payload)
      ElMessage.success('分类已更新')
    }
    else {
      await createCategory(payload)
      ElMessage.success('分类已新增')
    }

    dialogVisible.value = false
    await loadData()
  }
  finally {
    saving.value = false
  }
}

async function toggleStatus(item: Category) {
  if (item.readonly) {
    ElMessage.warning('社区广场预选菜系不允许修改')
    return
  }
  if (!item.id) return
  await updateCategory(item.id, {
    name: item.name,
    icon: item.icon,
    color: item.color,
    description: item.description,
    sortNo: item.sortNo || 0,
    status: item.status === 'disabled' ? 'enabled' : 'disabled',
  })
  await loadData()
}

async function handleDelete(item: Category) {
  if (item.readonly) {
    ElMessage.warning('社区广场预选菜系不允许删除')
    return
  }
  if (!item.id) return
  await ElMessageBox.confirm(`确认删除分类“${item.name}”吗？删除后用户端分类页不再展示。`, '删除确认', { type: 'warning' })
  await deleteCategory(item.id)
  ElMessage.success('分类已删除')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <PageHeader title="菜谱分类" description="维护用户端菜谱分类入口；社区广场八大菜系为固定预选项，不允许修改或删除。">
    <template #actions>
      <el-button @click="loadData">刷新分类</el-button>
      <el-button type="primary" @click="openCreate">新增分类</el-button>
    </template>
  </PageHeader>

  <div class="stats-grid">
    <article class="stat-card">
      <div class="stat-card__icon stat-card__icon--blue">🗂️</div>
      <div>
        <div class="stat-card__value">{{ groups.length }}</div>
        <div class="stat-card__label">分类分组</div>
      </div>
    </article>
    <article class="stat-card">
      <div class="stat-card__icon stat-card__icon--green">🏷️</div>
      <div>
        <div class="stat-card__value">{{ enabledCategories }}/{{ totalCategories }}</div>
        <div class="stat-card__label">启用分类</div>
      </div>
    </article>
    <article class="stat-card">
      <div class="stat-card__icon stat-card__icon--orange">🔒</div>
      <div>
        <div class="stat-card__value">{{ lockedCategories }}</div>
        <div class="stat-card__label">社区固定菜系</div>
      </div>
    </article>
    <article class="stat-card">
      <div class="stat-card__icon stat-card__icon--pink">📖</div>
      <div>
        <div class="stat-card__value">{{ totalRecipes }}</div>
        <div class="stat-card__label">关联菜谱数</div>
      </div>
    </article>
  </div>

  <div v-loading="loading" class="group-grid">
    <el-card v-for="group in groups" :key="group.code" class="app-card group-card">
      <div class="group-card__header">
        <div>
          <h3>{{ group.name }}</h3>
          <p>{{ group.children.length }} 个分类</p>
        </div>
        <el-tag v-if="group.code === 'chinese'" type="warning" effect="plain">社区预选固定</el-tag>
      </div>

      <div class="category-card-grid">
        <article v-for="item in group.children" :key="item.code" class="category-card" :class="{ 'is-disabled': item.status === 'disabled' }">
          <div class="category-card__icon" :style="{ background: item.color || '#f8fafc' }">
            {{ item.icon || '🏷️' }}
          </div>
          <div class="category-card__body">
            <div class="category-card__title-row">
              <div>
                <h4>{{ item.name }}</h4>
                <span>{{ item.code }}</span>
              </div>
              <div class="category-card__tags">
                <el-tag v-if="item.readonly" size="small" type="warning">锁定</el-tag>
                <el-tag size="small" :type="item.status === 'disabled' ? 'info' : 'success'">{{ statusLabel(item.status) }}</el-tag>
              </div>
            </div>
            <p>{{ item.description || '暂无分类描述' }}</p>
            <div class="category-card__footer">
              <span>排序 {{ item.sortNo || 0 }} · {{ item.recipeCount || 0 }} 个菜谱</span>
              <div class="category-card__actions">
                <el-button size="small" plain :disabled="item.readonly" @click="openEdit(item)">编辑</el-button>
                <el-button size="small" plain :disabled="item.readonly" @click="toggleStatus(item)">
                  {{ item.status === 'disabled' ? '启用' : '停用' }}
                </el-button>
                <el-button size="small" type="danger" plain :disabled="item.readonly" @click="handleDelete(item)">删除</el-button>
              </div>
            </div>
          </div>
        </article>
      </div>
    </el-card>

    <el-empty v-if="!groups.length && !loading" description="暂无分类数据" />
  </div>

  <el-dialog v-model="dialogVisible" class="admin-dialog category-dialog" :title="editingId ? '编辑分类' : '新增分类'" width="760px">
    <el-form label-position="top">
      <el-form-item label="分类名称" required>
        <el-input v-model="form.name" maxlength="32" placeholder="请输入分类名称，如：川菜" />
      </el-form-item>

      <el-form-item label="分类图标" required>
        <div class="icon-picker">
          <button
            v-for="icon in ICON_OPTIONS"
            :key="icon"
            type="button"
            class="icon-choice"
            :class="{ active: form.icon === icon }"
            @click="form.icon = icon"
          >
            {{ icon }}
          </button>
        </div>
      </el-form-item>

      <el-form-item label="图标颜色" required>
        <div class="color-picker">
          <button
            v-for="color in COLOR_OPTIONS"
            :key="color"
            type="button"
            class="color-choice"
            :class="{ active: form.color === color }"
            :style="{ background: color }"
            @click="form.color = color"
          />
        </div>
      </el-form-item>

      <el-form-item label="分类描述">
        <el-input
          v-model="form.description"
          maxlength="255"
          show-word-limit
          type="textarea"
          :rows="4"
          placeholder="请输入分类描述，简要说明该分类的特点..."
        />
      </el-form-item>

      <div class="admin-form-grid">
        <el-form-item label="排序号">
          <el-input v-model.number="form.sortNo" type="number" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="启用" value="enabled" />
            <el-option label="停用" value="disabled" />
          </el-select>
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitForm">
        {{ editingId ? '确认修改' : '确认添加' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.stat-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.stat-card__icon--blue { background: #e6f7ff; }
.stat-card__icon--green { background: #f6ffed; }
.stat-card__icon--orange { background: #fff7e6; }
.stat-card__icon--pink { background: #fff0f6; }

.stat-card__value {
  font-size: 22px;
  font-weight: 700;
  color: var(--admin-text-main);
}

.stat-card__label {
  margin-top: 4px;
  font-size: 12px;
  color: var(--admin-text-light);
}

.group-grid {
  display: grid;
  gap: 18px;
}

.group-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.group-card__header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--admin-text-main);
}

.group-card__header p {
  margin: 6px 0 0;
  color: var(--admin-text-light);
}

.category-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 14px;
}

.category-card {
  display: flex;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--admin-border);
  border-radius: 12px;
  background: #fff;
}

.category-card.is-disabled {
  opacity: 0.62;
}

.category-card__icon {
  width: 54px;
  height: 54px;
  flex: 0 0 54px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
}

.category-card__body {
  min-width: 0;
  flex: 1;
}

.category-card__title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.category-card__title-row h4 {
  margin: 0;
  font-size: 16px;
  color: var(--admin-text-main);
}

.category-card__title-row span {
  display: inline-block;
  margin-top: 4px;
  color: var(--admin-text-light);
  font-size: 12px;
}

.category-card__tags {
  display: flex;
  flex-shrink: 0;
  gap: 6px;
}

.category-card__body p {
  min-height: 42px;
  margin: 10px 0 12px;
  color: var(--admin-text-secondary);
  line-height: 1.6;
}

.category-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--admin-text-light);
  font-size: 12px;
}

.category-card__actions {
  display: flex;
  flex-shrink: 0;
  gap: 6px;
}

.icon-picker {
  display: grid;
  grid-template-columns: repeat(8, 48px);
  gap: 12px;
}

.icon-choice,
.color-choice {
  border: 0;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.icon-choice {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #fff;
  font-size: 24px;
}

.icon-choice.active,
.color-choice.active {
  box-shadow: 0 0 0 3px #fff, 0 0 0 5px var(--admin-primary);
}

.color-picker {
  display: flex;
  gap: 14px;
}

.color-choice {
  width: 52px;
  height: 52px;
  border-radius: 14px;
}

@media (max-width: 1180px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
