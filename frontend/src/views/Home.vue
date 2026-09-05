<template>
  <div class="home">
    <section class="hero">
      <div class="hero-text">
        <h2>你点的，正在路上～</h2>
        <p>进店逛逛，把好吃的加入购物车</p>
        <RouterLink class="hero-cta" to="/shops/1">进入示例店铺 →</RouterLink>
      </div>
      <div class="hero-emoji">🍔🍜🥡🍤</div>
    </section>

    <section class="coming">
      <h3>附近商家</h3>
      <div class="shop-list">
        <div class="shop-card">
          <span class="shop-emoji">🏪</span>
          <strong>商家占位</strong>
          <small>第二阶段开放营业</small>
        </div>
        <div class="shop-card">
          <span class="shop-emoji">🍜</span>
          <strong>面点速食</strong>
          <small>即将入驻</small>
        </div>
        <div class="shop-card">
          <span class="shop-emoji">🥤</span>
          <strong>奶茶甜品</strong>
          <small>即将入驻</small>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  background: var(--brand-gradient);
  color: #2b1d00;
  border-radius: var(--card-radius);
  padding: 2rem 2.5rem;
  box-shadow: 0 8px 24px rgba(255, 106, 0, 0.18);
}
.hero h2 {
  margin: 0 0 0.4rem;
  font-size: 1.55rem;
}
.hero p {
  margin: 0;
  opacity: 0.8;
}
.hero-emoji {
  font-size: 2.3rem;
  letter-spacing: 0.35rem;
}
.hero-cta {
  display: inline-block;
  margin-top: 0.8rem;
  background: #2b1d00;
  color: #ffd98a;
  padding: 0.45rem 1.2rem;
  border-radius: 999px;
  text-decoration: none;
  font-weight: 600;
}

.coming h3 {
  margin: 0 0 1rem;
}
.shop-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 1rem;
}
.shop-card {
  background: #fff;
  border-radius: var(--card-radius);
  padding: 1.25rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
  border: 1px dashed rgba(255, 106, 0, 0.4);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}
.shop-emoji {
  font-size: 2.1rem;
}
.shop-card small {
  color: #b0b0b0;
}
</style>
