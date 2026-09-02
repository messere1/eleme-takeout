<template>
  <header class="brand-bar">
    <div class="brand-inner">
      <div class="brand-title">
        <span class="brand-logo">🍜</span>
        <div>
          <h1>轻量级外卖服务平台</h1>
          <p class="brand-slogan">附近好店 · 准时送达</p>
        </div>
      </div>
      <nav class="brand-nav">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/login">登录</RouterLink>
        <RouterLink to="/register">注册</RouterLink>
      </nav>
    </div>
  </header>

  <!-- 当前路由对应的页面渲染在这里 -->
  <main class="shell">
    <RouterView />
  </main>

  <footer class="site-footer">轻量级外卖服务平台 · 软件工程综合实践</footer>
</template>

<style scoped>
.brand-bar {
  background: var(--brand-gradient);
  color: #2b1d00;
}
.brand-inner {
  max-width: 72rem;
  margin: 0 auto;
  padding: 0.9rem 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}
.brand-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.brand-logo {
  font-size: 1.9rem;
  line-height: 1;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  padding: 0.35rem 0.45rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.brand-title h1 {
  margin: 0;
  font-size: 1.3rem;
  line-height: 1.2;
}
.brand-slogan {
  margin: 0;
  font-size: 0.78rem;
  opacity: 0.7;
}
.brand-nav a {
  margin-left: 0.5rem;
  padding: 0.4rem 1.1rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #b34700;
  font-weight: 600;
  font-size: 0.92rem;
  text-decoration: none;
  transition: transform 0.12s ease;
}
.brand-nav a:hover {
  transform: translateY(-1px);
}
.brand-nav a.router-link-exact-active {
  background: #2b1d00;
  color: #ffd98a;
}

.shell {
  max-width: 72rem;
  margin: 0 auto;
  padding: 1.5rem;
  min-height: calc(100vh - 190px);
}

.site-footer {
  text-align: center;
  color: #b0b0b0;
  font-size: 0.8rem;
  padding: 1.25rem;
}
</style>
