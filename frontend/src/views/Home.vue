<script setup>
import { onMounted, ref } from 'vue'
import { listShops } from '@/api/shop'

// 当前后端暂无 GET /shops 列表接口，这里前端测试先放一组示例店铺看到滑动效果；
const DEMO_SHOPS = [
  { id: 1, shopName: '北洋餐厅', notice: '煎饼果子现做现卖，营业到 21:00', status: 'OPEN', emoji: '🥞', bg: '#fff1e8' },
  { id: 2, shopName: '川渝小馆', notice: '麻辣鲜香，满 30 减 5', status: 'OPEN', emoji: '🌶️', bg: '#fff0f0' },
  { id: 3, shopName: '茶百道奶茶', notice: '第二杯半价，今天点它', status: 'OPEN', emoji: '🧋', bg: '#f4f1ff' },
  { id: 4, shopName: '日料小屋', notice: '寿司拼盘 9 折上新', status: 'TEMP_CLOSED', emoji: '🍣', bg: '#eef8ff' },
  { id: 5, shopName: '深夜烧烤', notice: '21 点后营业，撸串走起', status: 'CLOSED', emoji: '🍢', bg: '#fff7ed' },
  { id: 6, shopName: '面包工坊', notice: '现烤吐司，早餐优选', status: 'OPEN', emoji: '🍞', bg: '#fdeef7' },
]

const EMOJI = ['🥞', '🌶️', '🧋', '🍣', '🍢', '🍞', '🍔', '🥡']
const BG = ['#fff1e8', '#fff0f0', '#f4f1ff', '#eef8ff', '#fff7ed', '#fdeef7']

const shops = ref([])
const demoOnly = ref(false)
const feedError = ref('')

const CATEGORIES = [
  { emoji: '🍜', name: '快餐' },
  { emoji: '🧋', name: '奶茶' },
  { emoji: '🥡', name: '小吃' },
  { emoji: '🍔', name: '汉堡' },
  { emoji: '🍣', name: '日料' },
  { emoji: '🍢', name: '烧烤' },
  { emoji: '🍰', name: '甜品' },
  { emoji: '🥗', name: '轻食' },
]

function decorate(shop, index) {
  return {
    ...shop,
    emoji: shop.emoji || EMOJI[index % EMOJI.length],
    bg: shop.bg || BG[index % BG.length],
  }
}

function statusText(status) {
  return { OPEN: '营业中', CLOSED: '休息中', TEMP_CLOSED: '临时闭店' }[status] || status
}

const currentPage = ref(1)
const totalPages = ref(1)
const feedLoading = ref(false)

async function load(page) {
  feedLoading.value = true
  feedError.value = ''
  try {
    const data = await listShops({ page, size: 20 })
    const items = data?.items || []
    shops.value = items.map(decorate)
    currentPage.value = data?.page ?? page
    totalPages.value = data?.totalPages ?? 1
    demoOnly.value = false
    if (items.length === 0) feedError.value = '暂时没有可展示的店铺'
  } catch {
    // 后端列表接口未就绪：退回示例店铺保证浏览体验
    shops.value = DEMO_SHOPS.map((shop, index) => decorate(shop, index))
    currentPage.value = 1
    totalPages.value = 1
    demoOnly.value = true
    feedError.value = '后端店铺列表接口未就绪，当前展示示例店铺'
  } finally {
    feedLoading.value = false
  }
}

onMounted(() => load(1))
</script>

<template>
  <div class="flash-home">
    <!-- 顶部促销条 -->
    <section class="flash-hero">
      <div class="hero-brand">
        <span class="bolt">⚡</span>
        <div>
          <h2>外卖闪购</h2>
          <p>附近好店 · 天天低价</p>
        </div>
      </div>
    </section>

    <!-- 搜索框 -->
    <div class="search-box">
      <span class="search-icon">🔍</span>
      <input class="search-input" type="search" placeholder="搜索想吃的美食" aria-label="搜索" />
    </div>

    <!-- 频道条 -->
    <section class="channel">
      <div v-for="item in CATEGORIES" :key="item.name" class="channel-chip">
        <span class="chip-emoji">{{ item.emoji }}</span>
        <span>{{ item.name }}</span>
      </div>
    </section>

    <!-- 附近店铺流 -->
    <section class="shop-feed">
      <header class="section-head">
        <span class="section-title">🏪 附近店铺</span>
        <span class="feed-count">共 {{ shops.length }} 家</span>
      </header>
      <p v-if="feedError" data-testid="home-feed-note" class="feed-note">{{ feedError }}</p>

      <div class="shop-stream">
        <RouterLink
          v-for="shop in shops"
          :key="shop.id"
          :to="`/shops/${shop.id}`"
          :data-testid="`home-shop-${shop.id}`"
          class="shop-card"
        >
          <div class="shop-thumb" :style="{ background: shop.bg }">{{ shop.emoji }}</div>
          <div class="shop-body">
            <div class="shop-line">
              <strong>{{ shop.shopName }}</strong>
              <span
                class="shop-status"
                :class="{ open: shop.status === 'OPEN' }"
              >{{ statusText(shop.status) }}</span>
            </div>
            <p class="shop-notice">{{ shop.notice }}</p>
            <div class="shop-tags">
              <span class="tag">外卖专送</span>
              <span class="tag">满减优惠</span>
            </div>
          </div>
          <span class="enter">进店 ›</span>
        </RouterLink>
      </div>

      <div v-if="shops.length" class="feed-pager">
        <button
          class="page-btn"
          data-testid="home-prev"
          :disabled="currentPage <= 1 || feedLoading"
          @click="load(currentPage - 1)"
        >上一页</button>
        <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
        <button
          class="page-btn"
          data-testid="home-next"
          :disabled="currentPage >= totalPages || feedLoading"
          @click="load(currentPage + 1)"
        >下一页</button>
      </div>
    </section>

    <!-- 热卖推荐（示例） -->
    <section class="deal-section">
      <header class="section-head">
        <span class="section-title">🔥 热卖推荐</span>
      </header>
      <div class="deal-grid">
        <RouterLink v-for="(deal, i) in ['煎饼果子', '珍珠奶茶', '照烧鸡排饭', '生煎包']" :key="deal" to="/shops/1" class="deal-card">
          <div class="deal-thumb">{{ ['🍜', '🧋', '🍱', '🥟'][i] }}</div>
          <div class="deal-info">
            <span class="deal-name">{{ deal }}</span>
            <span class="deal-price">到店尝鲜</span>
          </div>
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.flash-home {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

/* 顶部促销条 */
.flash-hero {
  background: linear-gradient(135deg, #ff5000 0%, #ff2f00 60%, #e82200 100%);
  color: #fff;
  border-radius: var(--card-radius);
  padding: 1.1rem 1.25rem;
  box-shadow: 0 8px 22px rgba(255, 47, 0, 0.28);
}
.hero-brand {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}
.bolt {
  font-size: 2rem;
}
.hero-brand h2 {
  margin: 0;
  font-size: 1.45rem;
  font-weight: 800;
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
}
.hero-brand p {
  margin: 0.1rem 0 0;
  font-size: 0.8rem;
  opacity: 0.9;
}

/* 搜索框 */
.search-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: #fff;
  border: 1px solid transparent;
  border-radius: 999px;
  padding: 0.55rem 1rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.search-box:focus-within {
  border-color: #ff5000;
  box-shadow: 0 0 0 3px rgba(255, 80, 0, 0.12);
}
.search-icon {
  font-size: 1rem;
  line-height: 1;
}
.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 0.95rem;
  color: #333;
}
.search-input::placeholder {
  color: #bbb;
}

/* 频道条 */
.channel {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 0.5rem;
  background: #fff;
  border-radius: var(--card-radius);
  padding: 0.9rem 0.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.channel-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.78rem;
  color: #444;
}
.chip-emoji {
  font-size: 1.4rem;
}

/* 标题通用 */
.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 0 0.2rem;
}
.section-title {
  font-size: 1.15rem;
  font-weight: 800;
  color: #2b1200;
}
.feed-count {
  color: #aaa;
  font-size: 0.8rem;
}

/* 店铺流 */
.shop-feed {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.feed-note {
  margin: 0;
  color: #ff7a18;
  font-size: 0.82rem;
}
.shop-stream {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.shop-card {
  display: flex;
  align-items: center;
  gap: 0.9rem;
  background: #fff;
  border-radius: var(--card-radius);
  padding: 0.8rem;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  text-decoration: none;
  color: inherit;
}
.shop-thumb {
  flex-shrink: 0;
  width: 3.9rem;
  height: 3.9rem;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
}
.shop-body {
  flex: 1;
  min-width: 0;
}
.shop-line {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.shop-line strong {
  font-size: 1.02rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.shop-status {
  flex-shrink: 0;
  font-size: 0.72rem;
  background: #f2f3f5;
  color: #666;
  border-radius: 999px;
  padding: 0.1rem 0.5rem;
}
.shop-status.open {
  background: #fff1e8;
  color: #ff6a00;
}
.shop-notice {
  margin: 0.25rem 0 0;
  color: #999;
  font-size: 0.85rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.shop-tags {
  margin-top: 0.4rem;
  display: flex;
  gap: 0.4rem;
}
.tag {
  font-size: 0.72rem;
  color: #ff6a00;
  background: #fff4ec;
  border-radius: 4px;
  padding: 0.05rem 0.4rem;
}
.enter {
  flex-shrink: 0;
  color: #ff5000;
  font-weight: 700;
  font-size: 0.95rem;
}

/* 热卖推荐 */
.deal-section {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.deal-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 0.75rem;
}
.feed-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
}
.page-btn {
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 999px;
  padding: 0.35rem 1rem;
  cursor: pointer;
  color: #444;
}
.page-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.page-info {
  color: #999;
  font-size: 0.9rem;
}
.deal-card {
  background: #fff;
  border-radius: var(--card-radius);
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.deal-thumb {
  text-align: center;
  font-size: 2.6rem;
  padding: 0.9rem 0 0.6rem;
  background: #fff4ec;
}
.deal-info {
  padding: 0.5rem 0.7rem 0.7rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.deal-name {
  font-weight: 600;
  font-size: 0.95rem;
}
.deal-price {
  color: #ff2f00;
  font-size: 0.8rem;
}
</style>
