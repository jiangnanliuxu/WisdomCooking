<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaptcha } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const captchaEnabled = ref(true)
const captchaImage = ref('')
const loginForm = reactive({
  username: 'admin',
  password: '',
  code: '',
  uuid: '',
})

async function loadCaptcha() {
  const response = await getCaptcha()
  captchaEnabled.value = response.captchaEnabled !== false
  captchaImage.value = response.img ? `data:image/gif;base64,${response.img}` : ''
  loginForm.uuid = String(response.uuid || '')
}

async function handleLogin() {
  loading.value = true
  try {
    await authStore.loginByPassword(loginForm)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.replace(redirect)
  }
  finally {
    loading.value = false
    if (captchaEnabled.value) {
      await loadCaptcha()
      loginForm.code = ''
    }
  }
}

onMounted(loadCaptcha)
</script>

<template>
  <div
    style="min-height: 100vh; display: grid; place-items: center; background: radial-gradient(circle at top, #fff2e6 0%, #eef2ff 50%, #e2e8f0 100%);"
  >
    <div style="display: grid; grid-template-columns: 420px 420px; overflow: hidden; border-radius: 18px; background: rgba(255,255,255,0.94); box-shadow: 0 24px 64px rgba(15, 23, 42, 0.12);">
      <div style="padding: 40px 34px; background: linear-gradient(135deg, #111827, #1e293b); color: #fff;">
        <div style="font-size: 30px; margin-bottom: 18px;">🍳</div>
        <div style="font-size: 28px; font-weight: 700; line-height: 1.2;">码上智厨<br>内容运营后台</div>
        <div style="margin-top: 16px; line-height: 1.8; color: rgba(255,255,255,0.72);">
          面向菜谱审核、社区运营、AI 模型配置和日志追踪的独立管理端。
        </div>
        <div style="margin-top: 28px; display: grid; gap: 12px;">
          <div style="padding: 12px 14px; border-radius: 12px; background: rgba(255,255,255,0.08);">AI 模型与日志统一管理</div>
          <div style="padding: 12px 14px; border-radius: 12px; background: rgba(255,255,255,0.08);">菜谱与动态审核联动</div>
          <div style="padding: 12px 14px; border-radius: 12px; background: rgba(255,255,255,0.08);">复用若依权限和网关体系</div>
        </div>
      </div>

      <div style="padding: 40px 34px;">
        <div style="font-size: 24px; font-weight: 700; color: #111827;">管理员登录</div>
        <div style="margin-top: 6px; color: #6b7280;">使用若依后台账号登录，进入业务运营后台。</div>

        <el-form label-position="top" style="margin-top: 26px;" @submit.prevent="handleLogin">
          <el-form-item label="账号">
            <el-input v-model="loginForm.username" placeholder="请输入后台账号" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="loginForm.password" show-password placeholder="请输入登录密码" />
          </el-form-item>

          <el-form-item v-if="captchaEnabled" label="验证码">
            <div style="display: grid; grid-template-columns: 1fr 128px; gap: 12px; width: 100%;">
              <el-input v-model="loginForm.code" placeholder="请输入验证码" />
              <button
                type="button"
                style="padding: 0; border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; cursor: pointer; overflow: hidden;"
                @click="loadCaptcha"
              >
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" style="display: block; width: 100%; height: 40px; object-fit: cover;">
                <span v-else style="display: inline-flex; align-items: center; justify-content: center; width: 100%; height: 40px;">刷新</span>
              </button>
            </div>
          </el-form-item>

          <el-button :loading="loading" type="primary" size="large" style="width: 100%; margin-top: 10px;" @click="handleLogin">
            登录后台
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>
