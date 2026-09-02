<template>
  <main class="shell">
    <header class="topbar">
      <h1>轻量级外卖服务平台</h1>
      <nav>
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/login">登录</RouterLink>
        <RouterLink to="/register">注册</RouterLink>
      </nav>
    </header>
    <RouterView />
  </main>
</template>

<style scoped>
.shell {
  max-width: 72rem;
  margin: 0 auto;
  padding: 2rem;
  font-family: system-ui, sans-serif;
}
.topbar {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
nav a {
  margin-left: 1rem;
}
</style>
