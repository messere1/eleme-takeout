<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'

const router = useRouter()

const PHONE_RE = /^1[3-9]\d{9}$/
const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d).{6,64}$/

const form = reactive({ username: '', phone: '', password: '' })
const fieldErrors = reactive({ username: '', phone: '', password: '' })
const errorMessage = ref('')
const submitting = ref(false)

function clearErrors() {
  errorMessage.value = ''
  fieldErrors.username = ''
  fieldErrors.phone = ''
  fieldErrors.password = ''
}

async function submit() {
  clearErrors()

  const username = form.username.trim()
  let valid = true
  if (username.length < 3 || username.length > 30) {
    fieldErrors.username = '用户名需 3~30 个字符'
    valid = false
  }
  if (!PHONE_RE.test(form.phone)) {
    fieldErrors.phone = '手机号格式不正确'
    valid = false
  }
  if (!PASSWORD_RE.test(form.password)) {
    fieldErrors.password = '密码需 6~64 位且包含字母和数字'
    valid = false
  }
  if (!valid) return

  submitting.value = true
  try {
    await register({ username, phone: form.phone, password: form.password })
    router.push('/login')
  } catch (error) {
    const fields = error?.data?.fieldErrors
    if (fields) {
      if (fields.username) fieldErrors.username = fields.username
      if (fields.phone) fieldErrors.phone = fields.phone
      if (fields.password) fieldErrors.password = fields.password
    }
    errorMessage.value = error?.message || '注册失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <section class="auth-card">
      <header class="card-head">
        <span class="card-emoji">🥡</span>
        <h2>新用户注册</h2>
        <p class="card-tip">注册一个账号，开始你的第一单</p>
      </header>

      <div class="field">
        <label for="register-username">用户名</label>
        <el-input
          id="register-username"
          v-model="form.username"
          data-testid="register-username"
          class="auth-input"
          placeholder="3~30 个字符"
          @keyup.enter="submit"
        />
        <p v-if="fieldErrors.username" class="field-error">{{ fieldErrors.username }}</p>
      </div>

      <div class="field">
        <label for="register-phone">手机号</label>
        <el-input
          id="register-phone"
          v-model="form.phone"
          data-testid="register-phone"
          class="auth-input"
          placeholder="中国大陆手机号"
          maxlength="11"
          @keyup.enter="submit"
        />
        <p v-if="fieldErrors.phone" class="field-error">{{ fieldErrors.phone }}</p>
      </div>

      <div class="field">
        <label for="register-password">密码</label>
        <el-input
          id="register-password"
          v-model="form.password"
          type="password"
          data-testid="register-password"
          class="auth-input"
          placeholder="6~64 位，含字母和数字"
          show-password
          @keyup.enter="submit"
        />
        <p v-if="fieldErrors.password" class="field-error">{{ fieldErrors.password }}</p>
      </div>

      <p v-if="errorMessage" data-testid="register-error" role="alert" class="form-error">
        {{ errorMessage }}
      </p>

      <el-button
        type="primary"
        data-testid="register-submit"
        class="submit-btn"
        :disabled="submitting"
        @click="submit"
      >
        {{ submitting ? '注册中…' : '注册' }}
      </el-button>

      <p class="switch-line">
        已有账号？<RouterLink to="/login">去登录</RouterLink>
      </p>
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  max-width: 27rem;
  margin: 1.5rem auto 2.5rem;
}
.auth-card {
  background: #fff;
  border-radius: var(--card-radius);
  border-top: 5px solid var(--el-color-primary);
  padding: 2rem 1.9rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08);
}
.card-head {
  text-align: center;
  margin-bottom: 0.25rem;
}
.card-emoji {
  display: block;
  font-size: 2.3rem;
}
.card-head h2 {
  margin: 0.25rem 0 0;
  font-size: 1.45rem;
}
.card-tip {
  margin: 0.25rem 0 0;
  color: #b0b0b0;
  font-size: 0.85rem;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.field label {
  font-size: 0.85rem;
  color: #666;
}
.auth-input,
.auth-input :deep(.el-input) {
  width: 100%;
}
.field-error,
.form-error {
  margin: 0;
  color: #e34d1c;
  font-size: 0.88rem;
}
.submit-btn {
  width: 100%;
  height: 42px;
  font-size: 1rem;
  margin-top: 0.25rem;
}
.switch-line {
  text-align: center;
  margin: 0.5rem 0 0;
  color: #b0b0b0;
  font-size: 0.9rem;
}
.switch-line a {
  color: var(--el-color-primary);
  text-decoration: none;
  font-weight: 600;
}
</style>
