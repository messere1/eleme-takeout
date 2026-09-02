<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/auth'
import { session } from '@/utils/session'

const router = useRouter()

const form = reactive({ account: '', password: '' })
const errorMessage = ref('')
const submitting = ref(false)

async function submit() {
  errorMessage.value = ''
  const account = form.account.trim()
  if (!account || !form.password) {
    errorMessage.value = '请输入账号和密码'
    return
  }

  submitting.value = true
  try {
    const data = await login({ account, password: form.password, role: 'CUSTOMER' })
    session.save({ token: data.token, role: data.role || 'CUSTOMER' })
    router.push('/')
  } catch (error) {
    errorMessage.value = error?.message || '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="auth-card">
    <h2>用户登录</h2>

    <el-input
      v-model="form.account"
      data-testid="login-account"
      class="auth-field"
      placeholder="账号（用户名或手机号）"
      @keyup.enter="submit"
    />
    <el-input
      v-model="form.password"
      type="password"
      data-testid="login-password"
      class="auth-field"
      placeholder="密码"
      show-password
      @keyup.enter="submit"
    />

    <p v-if="errorMessage" data-testid="login-error" role="alert" class="auth-error">
      {{ errorMessage }}
    </p>

    <el-button
      type="primary"
      data-testid="login-submit"
      class="auth-submit"
      :disabled="submitting"
      @click="submit"
    >
      {{ submitting ? '登录中…' : '登录' }}
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
.auth-error {
  color: #f56c6c;
  margin: 0;
}
.auth-submit {
  width: 100%;
}
</style>
