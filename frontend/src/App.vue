<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { session } from '@/utils/session'

const router = useRouter()
const logged = ref(false)
const role = ref('')

function refresh() {
  const auth = session.load()
  logged.value = Boolean(auth)
  role.value = auth?.role || ''
}

function logout() {
  session.clear()
  router.push('/')
}

onMounted(() => {
  refresh()
  window.addEventListener('auth-change', refresh)
})
onBeforeUnmount(() => window.removeEventListener('auth-change', refresh))
</script>

<template>
  <header class="brand-bar">
    <div class="brand-inner">
      <RouterLink to="/" class="brand-title">
        <span class="brand-logo">🍜</span>
        <span class="brand-text">
          <h1>轻量级外卖服务平台</h1>
          <span class="brand-slogan">附近好店 · 准时送达</span>
        </span>
      </RouterLink>
      <button v-if="logged" class="nav-link logout" @click="logout">退出</button>
    </div>
  </header>

  <main class="shell">
    <RouterView />
  </main>

  <!-- 手机端底部 Tab：按角色显示（商家只见商家后台页，可随时回主界面） -->
  <nav v-if="role === 'MERCHANT'" class="tabbar merchant">
    <RouterLink to="/merchant" class="tab"><span class="ico">🏪</span><span>店铺</span></RouterLink>
    <RouterLink to="/merchant/products" class="tab"><span class="ico">🍽️</span><span>商品管理</span></RouterLink>
    <RouterLink to="/merchant/orders" class="tab"><span class="ico">📋</span><span>订单管理</span></RouterLink>
    <RouterLink to="/merchant/profile" class="tab"><span class="ico">👤</span><span>我的</span></RouterLink>
  </nav>
  <nav v-else class="tabbar">
    <RouterLink to="/" class="tab"><span class="ico">🏠</span><span>首页</span></RouterLink>
    <RouterLink to="/cart" class="tab"><span class="ico">🛒</span><span>购物车</span></RouterLink>
    <RouterLink to="/orders" class="tab"><span class="ico">📋</span><span>订单</span></RouterLink>
    <RouterLink to="/profile" class="tab"><span class="ico">👤</span><span>我的</span></RouterLink>
  </nav>
</template>

<style scoped>
.brand-bar {
  background: var(--brand-gradient);
  color: #2b1d00;
}
.brand-inner {
  max-width: 48rem;
  margin: 0 auto;
  padding: 0.7rem 1rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}
.brand-title {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  text-decoration: none;
  color: inherit;
}
.brand-logo {
  font-size: 1.7rem;
  line-height: 1;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  padding: 0.3rem 0.45rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.brand-text {
  display: flex;
  flex-direction: column;
}
.brand-text h1 {
  margin: 0;
  font-size: 1.15rem;
  line-height: 1.2;
}
.brand-slogan {
  font-size: 0.75rem;
  opacity: 0.7;
}
.nav-link.logout {
  border: none;
  background: rgba(255, 255, 255, 0.92);
  color: #b34700;
  font-weight: 600;
  font-size: 0.9rem;
  border-radius: 999px;
  padding: 0.35rem 1rem;
  cursor: pointer;
}
.shell {
  max-width: 48rem;
  margin: 0 auto;
  padding: 1rem 1rem calc(4.6rem);
  min-height: calc(100vh - 7rem);
}
.tabbar {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  bottom: 0;
  width: 100%;
  max-width: 48rem;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  z-index: 20;
  padding-bottom: env(safe-area-inset-bottom);
}
.tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 0.5rem 0 0.55rem;
  color: #999;
  text-decoration: none;
  font-size: 0.7rem;
}
.tab .ico {
  font-size: 1.2rem;
  line-height: 1;
}
.tab.router-link-exact-active {
  color: #ff5000;
  font-weight: 600;
}
</style>
