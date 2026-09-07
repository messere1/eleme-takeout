<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/auth'
import { session } from '@/utils/session'

const router = useRouter()

const form = reactive({ account: '', password: '', role: 'CUSTOMER' })
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
    const data = await login({
      account,
      password: form.password,
      role: form.role,
    })
    const role = data.role || form.role
    session.save({ token: data.token, role })
    router.push(role === 'MERCHANT' ? '/merchant' : '/')
  } catch (error) {
    errorMessage.value = error?.message || '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <section class="auth-card">
      <header class="card-head">
        <span class="card-emoji">🍜</span>
        <h2>欢迎回来</h2>
        <p class="card-tip">登录后开始点单</p>
      </header>

      <div class="field">
        <label for="login-account">账号</label>
        <el-input
          id="login-account"
          v-model="form.account"
          data-testid="login-account"
          class="auth-input"
          placeholder="手机号或用户名"
          @keyup.enter="submit"
        />
      </div>

      <div class="field">
        <label for="login-password">密码</label>
        <el-input
          id="login-password"
          v-model="form.password"
          type="password"
          data-testid="login-password"
          class="auth-input"
          placeholder="请输入密码"
          show-password
          @keyup.enter="submit"
        />
      </div>

      <div class="field">
        <label for="login-role">登录身份</label>
        <select
          id="login-role"
          v-model="form.role"
          data-testid="login-role"
          style="width:100%;border:1px solid #dcdfe6;border-radius:8px;padding:0.55rem 0.75rem;background:#fff;color:#2b2b2b;font-size:0.95rem;"
        >
          <option value="CUSTOMER">我是顾客</option>
          <option value="MERCHANT">我是商家</option>
        </select>
      </div>

      <p v-if="errorMessage" data-testid="login-error" role="alert" class="form-error">
        {{ errorMessage }}
      </p>

      <el-button
        type="primary"
        data-testid="login-submit"
        class="submit-btn"
        :disabled="submitting"
        @click="submit"
      >
        {{ submitting ? '登录中…' : '登录' }}
      </el-button>

      <p class="switch-line">
        还没有账号？<RouterLink to="/register">去注册</RouterLink>
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
  gap: 0.4rem;
}
.field label {
  font-size: 0.85rem;
  color: #666;
}
.auth-input,
.auth-input :deep(.el-input) {
  width: 100%;
}
.form-error {
  margin: 0;
  color: #e34d1c;
  font-size: 0.9rem;
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
