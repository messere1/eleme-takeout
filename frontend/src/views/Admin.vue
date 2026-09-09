<script setup>
import { onMounted, ref } from 'vue'
import { listUsers } from '@/api/user'
import { listMerchants } from '@/api/merchant'

const tab = ref('users')
const users = ref([])
const merchants = ref([])
const error = ref('')

async function loadUsers() {
  try {
    const data = await listUsers()
    users.value = data?.items ?? []
  } catch {
    error.value = '管理接口暂不可用（后端待提供）'
  }
}

async function loadMerchants() {
  try {
    const data = await listMerchants()
    merchants.value = data?.items ?? []
  } catch {
    error.value = '管理接口暂不可用（后端待提供）'
  }
}

async function refresh() {
  error.value = ''
  if (tab.value === 'users') await loadUsers()
  else await loadMerchants()
}

onMounted(refresh)
</script>

<template>
  <section class="admin">
    <h2>系统管理</h2>
    <p class="admin-tip">独立管理页，无顾客/商家入口（ROLE_ADMIN，后端待接入）</p>
    <nav class="tabs">
      <button class="tab" :class="{ active: tab === 'users' }" @click="tab = 'users'; refresh()">用户</button>
      <button class="tab" :class="{ active: tab === 'merchants' }" @click="tab = 'merchants'; refresh()">商家</button>
    </nav>
    <p v-if="error" class="admin-error">{{ error }}</p>
    <table v-if="tab === 'users' && users.length" class="admin-table">
      <thead><tr><th>ID</th><th>用户名</th><th>手机号</th><th>昵称</th></tr></thead>
      <tbody>
        <tr v-for="u in users" :key="u.id"><td>{{ u.id }}</td><td>{{ u.username }}</td><td>{{ u.phone }}</td><td>{{ u.nickname }}</td></tr>
      </tbody>
    </table>
    <table v-else-if="tab === 'merchants' && merchants.length" class="admin-table">
      <thead><tr><th>ID</th><th>商家名称</th><th>手机号</th><th>经营范围</th></tr></thead>
      <tbody>
        <tr v-for="m in merchants" :key="m.id"><td>{{ m.id }}</td><td>{{ m.merchantName }}</td><td>{{ m.phone }}</td><td>{{ m.businessScope }}</td></tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.admin {
  max-width: 60rem;
  margin: 0 auto;
  background: #fff;
  border-radius: var(--card-radius);
  padding: 1.5rem;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.06);
}
.admin h2 { margin: 0; }
.admin-tip { color: #b0b0b0; font-size: 0.85rem; }
.tabs { display: flex; gap: 0.5rem; margin: 1rem 0; }
.tab { border: 1px solid #eee; background: #fff; padding: 0.4rem 1.2rem; border-radius: 999px; cursor: pointer; }
.tab.active { background: var(--brand-gradient); color: #fff; border-color: transparent; }
.admin-error { color: #e34d1c; }
.admin-table { width: 100%; border-collapse: collapse; }
.admin-table th, .admin-table td { border: 1px solid #eee; padding: 0.5rem 0.75rem; text-align: left; }
.admin-table th { background: #fafafa; }
</style>
