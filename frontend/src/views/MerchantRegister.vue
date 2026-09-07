<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { registerMerchant } from '@/api/merchant'
import { session } from '@/utils/session'

const router = useRouter()

const PHONE_RE = /^1[3-9]\d{9}$/
const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d).{6,64}$/

const form = reactive({ merchantName: '', phone: '', password: '', businessScope: '' })
const errorMessage = ref('')
const submitting = ref(false)

async function submit() {
  errorMessage.value = ''

  const merchantName = form.merchantName.trim()
  const businessScope = form.businessScope.trim()
  let invalid = ''
  if (!merchantName || merchantName.length > 50) {
    invalid = '请输入商家名称（最多 50 字）'
  } else if (!PHONE_RE.test(form.phone)) {
    invalid = '手机号格式不正确'
  } else if (!PASSWORD_RE.test(form.password)) {
    invalid = '密码需 6~64 位且包含字母和数字'
  } else if (!businessScope) {
    invalid = '请输入经营范围'
  }
  if (invalid) {
    errorMessage.value = invalid
    return
  }

  submitting.value = true
  try {
    const data = await registerMerchant({
      merchantName,
      phone: form.phone,
      password: form.password,
      businessScope,
    })
    // 登录接口不回 shopId，注册响应里缓存“我的店铺”供后台定位。
    session.saveShop({
      merchantId: data.id,
      shopId: data.shopId,
      merchantName: data.merchantName,
    })
    router.push('/merchant/login')
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
        <span class="card-emoji">🏪</span>
        <h2>商家入驻</h2>
        <p class="card-tip">注册成功后自动为你创建店铺</p>
      </header>

      <div class="field">
        <label for="mr-name">商家名称</label>
        <el-input
          id="mr-name"
          v-model="form.merchantName"
          data-testid="mr-name"
          class="auth-input"
          placeholder="最多 50 字"
          maxlength="50"
        />
      </div>

      <div class="field">
        <label for="mr-phone">手机号</label>
        <el-input
          id="mr-phone"
          v-model="form.phone"
          data-testid="mr-phone"
          class="auth-input"
          placeholder="11 位手机号"
          maxlength="11"
        />
      </div>

      <div class="field">
        <label for="mr-password">密码</label>
        <el-input
          id="mr-password"
          v-model="form.password"
          type="password"
          data-testid="mr-password"
          class="auth-input"
          placeholder="6~64 位，含字母和数字"
          show-password
        />
      </div>

      <div class="field">
        <label for="mr-scope">经营范围</label>
        <el-input
          id="mr-scope"
          v-model="form.businessScope"
          data-testid="mr-scope"
          class="auth-input"
          placeholder="如：中式快餐"
          maxlength="100"
        />
      </div>

      <p v-if="errorMessage" data-testid="mr-message" role="alert" class="form-error">
        {{ errorMessage }}
      </p>

      <el-button
        type="primary"
        data-testid="mr-submit"
        class="submit-btn"
        :disabled="submitting"
        @click="submit"
      >
        {{ submitting ? '注册中…' : '注册店铺' }}
      </el-button>

      <p class="switch-line">
        已有店铺？<RouterLink to="/merchant/login">去商家登录</RouterLink>
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
