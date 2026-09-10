<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getProfile, updateProfile } from '@/api/user'

const form = reactive({ nickname: '', phone: '', address: '' })
const username = ref('')
const message = ref('')
const saving = ref(false)
const avatar = ref('')

function onAvatarPick(event) {
  const file = event.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    avatar.value = String(reader.result)
  }
  reader.readAsDataURL(file)
}

async function load() {
  try {
    const me = await getProfile()
    username.value = me.username || ''
    form.nickname = me.nickname || ''
    form.phone = me.phone || ''
    form.address = me.address || ''
  } catch (error) {
    message.value = error?.message || '无法加载资料，请稍后重试'
  }
}

async function save() {
  message.value = ''
  saving.value = true
  try {
    await updateProfile({
      nickname: form.nickname.trim(),
      phone: form.phone.trim(),
      address: form.address.trim(),
    })
    message.value = '已保存'
  } catch (error) {
    message.value = error?.message || '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="profile-page">
    <h2>个人资料</h2>
    <p v-if="username" class="account-line">账号：{{ username }}</p>

    <div class="avatar-row">
      <img v-if="avatar" :src="avatar" class="avatar-preview" alt="头像预览" />
      <div v-else class="avatar-placeholder">头像</div>
      <label class="avatar-btn">
        上传头像
        <input type="file" accept="image/*" data-testid="profile-avatar-input" @change="onAvatarPick" />
      </label>
    </div>

    <div class="field">
      <label for="profile-nickname">昵称</label>
      <el-input
        id="profile-nickname"
        v-model="form.nickname"
        data-testid="profile-nickname"
        placeholder="昵称（最多 30 字）"
        maxlength="30"
      />
    </div>

    <div class="field">
      <label for="profile-phone">手机号</label>
      <el-input
        id="profile-phone"
        v-model="form.phone"
        data-testid="profile-phone"
        placeholder="11 位手机号"
        maxlength="11"
      />
    </div>

    <div class="field">
      <label for="profile-address">收货地址</label>
      <el-input
        id="profile-address"
        v-model="form.address"
        data-testid="profile-address"
        placeholder="收货地址（最多 255 字）"
        maxlength="255"
      />
    </div>

    <p v-if="message" data-testid="profile-message" role="status" class="profile-message">
      {{ message }}
    </p>

    <el-button
      type="primary"
      data-testid="profile-save"
      class="save-btn"
      :disabled="saving"
      @click="save"
    >
      {{ saving ? '保存中…' : '保存' }}
    </el-button>
  </section>
</template>

<style scoped>
.profile-page {
  max-width: 30rem;
  margin: 1.5rem auto 2.5rem;
  background: #fff;
  border-radius: var(--card-radius);
  padding: 1.75rem 1.9rem;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.profile-page h2 {
  margin: 0;
}
.account-line {
  margin: 0;
  color: #999;
  font-size: 0.85rem;
}
.avatar-row {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.avatar-preview,
.avatar-placeholder {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f2f3f5;
  color: #999;
}
.avatar-btn {
  color: var(--el-color-primary);
  cursor: pointer;
  font-size: 0.9rem;
}
.avatar-btn input {
  display: none;
}
.avatar-row {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.avatar-preview,
.avatar-placeholder {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f2f3f5;
  color: #999;
}
.avatar-btn {
  color: var(--el-color-primary);
  cursor: pointer;
  font-size: 0.9rem;
}
.avatar-btn input {
  display: none;
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
.field :deep(.el-input) {
  width: 100%;
}
.profile-message {
  margin: 0;
  color: #ff6a00;
  font-weight: 600;
}
.save-btn {
  align-self: flex-start;
}
@media (max-width: 520px) {
  .profile-page {
    padding: 1rem;
  }
  .avatar-row {
    flex-wrap: wrap;
  }
}
</style>
