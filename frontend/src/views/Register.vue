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
  <section class="auth-card">
    <h2>用户注册</h2>

    <div class="field">
      <el-input
        v-model="form.username"
        data-testid="register-username"
        placeholder="用户名（3~30 个字符）"
        @keyup.enter="submit"
      />
      <p v-if="fieldErrors.username" class="field-error">{{ fieldErrors.username }}</p>
    </div>

    <div class="field">
      <el-input
        v-model="form.phone"
        data-testid="register-phone"
        placeholder="手机号"
        maxlength="11"
        @keyup.enter="submit"
      />
      <p v-if="fieldErrors.phone" class="field-error">{{ fieldErrors.phone }}</p>
    </div>

    <div class="field">
      <el-input
        v-model="form.password"
        type="password"
        data-testid="register-password"
        placeholder="密码（6~64 位，含字母和数字）"
        show-password
        @keyup.enter="submit"
      />
      <p v-if="fieldErrors.password" class="field-error">{{ fieldErrors.password }}</p>
    </div>

    <p v-if="errorMessage" data-testid="register-error" role="alert" class="auth-error">
      {{ errorMessage }}
    </p>

    <el-button
      type="primary"
      data-testid="register-submit"
      class="auth-submit"
      :disabled="submitting"
      @click="submit"
    >
      {{ submitting ? '注册中…' : '注册' }}
    </el-button>
  </section>
</template>

<style scoped>
.auth-card {
  max-width: 24rem;
  margin: 2rem auto;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.field-error,
.auth-error {
  color: #f56c6c;
  margin: 0;
  font-size: 0.875rem;
}
.auth-submit {
  width: 100%;
}
</style>
