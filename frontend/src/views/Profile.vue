<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getProfile, updateProfile, deleteAccount } from '@/api/user'
import ImageUploader from '@/components/ImageUploader.vue'
import { session } from '@/utils/session'
import { useRouter } from 'vue-router'

const form = reactive({ nickname: '', phone: '', address: '' })
const username = ref('')
const message = ref('')
const saving = ref(false)
const avatar = ref('')
const userId = ref(null)
const router = useRouter()

async function load() {
  try {
    const me = await getProfile()
    username.value = me.username || ''
    userId.value = me.id
    avatar.value = me.avatarUrl || ''
    form.nickname = me.nickname || ''
    form.phone = me.phone || ''
    form.address = me.address || ''
  } catch (error) {
    message.value = error?.message || '无法加载资料，请稍后重试'
  }
}

async function removeAccount(){
  if(!window.confirm('确认注销账号？存在进行中订单时将无法注销。')) return
  try{await deleteAccount();session.clear();router.push('/login')}catch(e){message.value=e?.message||'注销失败'}
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

    <ImageUploader
      v-model="avatar"
      target-type="USER_AVATAR"
      :target-id="userId"
      shape="round"
      placeholder="头像"
      upload-label="上传头像"
      replace-label="更换头像"
      input-testid="profile-avatar-input"
      @message="message = $event"
    />

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
    <button class="delete-account" data-testid="profile-delete" @click="removeAccount">注销账号</button>
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
.delete-account{align-self:flex-start;border:0;background:transparent;color:#c0392b;cursor:pointer}
@media (max-width: 520px) {
  .profile-page {
    padding: 1rem;
  }
}
</style>
