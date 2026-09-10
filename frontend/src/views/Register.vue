<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login as apiLogin, register as apiRegister } from '@/api/auth'
import { registerMerchant } from '@/api/merchant'
import { session } from '@/utils/session'

const router = useRouter()

const PHONE_RE = /^1[3-9]\d{9}$/
const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d).{6,64}$/

const form = reactive({
  role: 'CUSTOMER',
  username: '',
  merchantName: '',
  phone: '',
  password: '',
  businessScope: '',
})
const errorMessage = ref('')
const submitting = ref(false)

function invalidMessage() {
  if (!PHONE_RE.test(form.phone.trim())) return '手机号格式不正确'
  if (!PASSWORD_RE.test(form.password)) return '密码需 6~64 位且包含字母和数字'
  if (form.role === 'CUSTOMER') {
    const username = form.username.trim()
    if (username.length < 3 || username.length > 30) return '用户名需 3~30 个字符'
  } else {
    const name = form.merchantName.trim()
    if (!name || name.length > 50) return '请输入商家名称（最多 50 字）'
    if (!form.businessScope.trim()) return '请输入经营范围'
  }
  return ''
}

async function autoLogin(phone, password, role) {
  const data = await apiLogin({ account: phone, password, role })
  const finalRole = data.role || role
  session.save({ token: data.token, role: finalRole })
  router.push(finalRole === 'MERCHANT' ? '/merchant' : finalRole === 'ADMIN' ? '/admin' : '/')
}

async function submit() {
  errorMessage.value = ''
  const invalid = invalidMessage()
  if (invalid) {
    errorMessage.value = invalid
    return
  }

  const phone = form.phone.trim()
  const password = form.password
  submitting.value = true
  try {
    if (form.role === 'CUSTOMER') {
      await apiRegister({ username: form.username.trim(), phone, password })
    } else {
      const data = await registerMerchant({
        merchantName: form.merchantName.trim(),
        phone,
        password,
        businessScope: form.businessScope.trim(),
      })
      session.saveShop({
        merchantId: data.id,
        shopId: data.shopId,
        merchantName: data.merchantName,
        merchantPhone: form.phone,
        businessScope: form.businessScope.trim(),
      })
    }
    // 注册成功后自动登录，并按角色进入对应主页面
    await autoLogin(phone, password, form.role)
  } catch (error) {
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
        <span class="card-emoji">{{ form.role === 'MERCHANT' ? '🏪' : '🍜' }}</span>
        <h2>注册</h2>
        <p class="card-tip">注册成功后将自动登录</p>
      </header>

      <div class="field">
        <label for="register-role">注册身份</label>
        <select
          id="register-role"
          v-model="form.role"
          data-testid="register-role"
          class="role-select"
        >
          <option value="CUSTOMER">我是顾客</option>
          <option value="MERCHANT">我是商家</option>
        </select>
      </div>

      <template v-if="form.role === 'CUSTOMER'">
        <div class="field">
          <label for="register-username">用户名</label>
          <el-input
            id="register-username"
            v-model="form.username"
            data-testid="register-username"
            class="auth-input"
            placeholder="3~30 个字符"
            maxlength="30"
          />
        </div>
      </template>
      <template v-else>
        <div class="field">
          <label for="register-merchant-name">商家名称</label>
          <el-input
            id="register-merchant-name"
            v-model="form.merchantName"
            data-testid="register-merchant-name"
            class="auth-input"
            placeholder="最多 50 字"
            maxlength="50"
          />
        </div>
      </template>

      <div class="field">
        <label for="register-phone">手机号</label>
        <el-input
          id="register-phone"
          v-model="form.phone"
          data-testid="register-phone"
          class="auth-input"
          placeholder="11 位手机号"
          maxlength="11"
        />
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
        />
      </div>

      <template v-if="form.role === 'MERCHANT'">
        <div class="field">
          <label for="register-scope">经营类别</label>
          <el-input
            id="register-scope"
            v-model="form.businessScope"
            data-testid="register-scope"
            class="auth-input"
            list="merchant-scope-list"
            placeholder="选择或输入自定义类别，如：中式快餐"
            maxlength="100"
          />
          <datalist id="merchant-scope-list">
            <option value="中式快餐"></option>
            <option value="西式简餐"></option>
            <option value="奶茶甜品"></option>
            <option value="烧烤夜宵"></option>
            <option value="日韩料理"></option>
            <option value="地方菜系"></option>
          </datalist>
        </div>
      </template>

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
  gap: 0.4rem;
}
.field label {
  font-size: 0.85rem;
  color: #666;
}
.role-select {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 0.55rem 0.75rem;
  background: #fff;
  color: #2b2b2b;
  font-size: 0.95rem;
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
@media (max-width: 520px) { .auth-page { margin: 1rem auto; } .auth-card { padding: 1.4rem 1.1rem; } }
</style>
