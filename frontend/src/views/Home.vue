<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listShops } from '@/api/shop'

const router = useRouter()

function goSearch() {
  router.push('/search')
}

const EMOJI = ['🥞', '🌶️', '🧋', '🍣', '🍢', '🍞', '🍔', '🥡']
const BG = ['#fff1e8', '#fff0f0', '#f4f1ff', '#eef8ff', '#fff7ed', '#fdeef7']

const shops = ref([])
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

async function load(page, append = false) {
  if (feedLoading.value) return
  feedLoading.value = true
  feedError.value = ''
  try {
    const data = await listShops({ page, size: 20 })
    const items = (data?.items || []).map(decorate)
    shops.value = append ? shops.value.concat(items) : items
    currentPage.value = data?.page ?? page
    totalPages.value = data?.totalPages ?? 1
    if (!append && items.length === 0) feedError.value = '暂时没有可展示的店铺'
  } catch {
    if (!append) {
      shops.value = []
      currentPage.value = 1
      totalPages.value = 1
      feedError.value = '店铺列表加载失败，请稍后重试'
    }
  } finally {
    feedLoading.value = false
  }
}

// 滚动接力：页面滚到店铺区标题吸顶前 → 滚整页；吸顶后 → 滚店铺列表；
// 列表到边界再继续滚 → 自动切回整页。列表本身不接原生滚动（overflow:hidden），
// 全部由这里的滚轮逻辑统一分配，才能做到“先滚页面再吸顶”。
const brandHeight = ref(56) // 动态测量品牌栏高度
const browseHeight = ref(0) // 店铺区可视高度：视口扣掉顶栏、吸顶搜索框和底栏
const searchBox = ref(null)
const browseBlock = ref(null)

// 店铺区高度按真实测量值算，不再写死 rem：写死会在字号/缩放/安全区变化时
// 比可用空间高出一截，底部被固定 Tab 挡住，最后一张卡片滚不出来。
function measureShell() {
  const bar = document.querySelector('.brand-bar')
  if (bar) brandHeight.value = bar.offsetHeight
  const tabbar = document.querySelector('.tabbar')
  const searchHeight = searchBox.value ? searchBox.value.offsetHeight : 0
  const reserved =
    brandHeight.value + searchHeight + (tabbar ? tabbar.offsetHeight : 0) + 12 // 12 = 店铺区与搜索框的间距
  browseHeight.value = Math.max(240, window.innerHeight - reserved)
}

const shellStyle = computed(() => ({
  '--brand-h': `${brandHeight.value}px`,
  ...(browseHeight.value ? { '--browse-h': `${browseHeight.value}px` } : {}),
}))

function maybeLoadMore() {
  const block = browseBlock.value
  if (!block) return
  const nearBottom = block.scrollTop + block.clientHeight >= block.scrollHeight - 60
  if (nearBottom && currentPage.value < totalPages.value) {
    load(currentPage.value + 1, true)
  }
}

function searchStuck() {
  const box = searchBox.value
  if (!box) return false
  // 搜索框吸到品牌栏下方
  return box.getBoundingClientRect().top <= brandHeight.value + 2
}

function onWindowWheel(event) {
  const block = browseBlock.value
  if (!block || event.deltaY === 0) return
  if (!searchStuck()) return // 搜索框未触顶：整页滚动

  const canDown = block.scrollTop + block.clientHeight < block.scrollHeight - 1
  const canUp = block.scrollTop > 0

  if (event.deltaY > 0) {
    // 往下：列表还能滚就滚列表，到底了才交回整页
    if (!canDown) return
    block.scrollTop += event.deltaY
    maybeLoadMore()
  } else {
    // 往上：列表没到顶就滚列表，到顶了才交回整页
    if (!canUp) return
    block.scrollTop += event.deltaY
  }

  // 列表吃掉这次滚动后必须拦掉默认行为：否则整页会跟着一起滚，店铺区被顶到
  // 吸顶搜索框下面，表现为滚动错位、顶部/底部被截断。
  event.preventDefault()
}

onMounted(() => {
  measureShell()
  load(1)
  window.addEventListener('wheel', onWindowWheel, { passive: false })
  window.addEventListener('resize', measureShell)
})
onUnmounted(() => {
  window.removeEventListener('wheel', onWindowWheel)
  window.removeEventListener('resize', measureShell)
})
</script>

<template>
  <div class="flash-home" :style="shellStyle">
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

    <!-- 搜索框（吸顶）：点击进入搜索页 -->
    <div
      ref="searchBox"
      class="search-box"
      data-testid="home-search-entry"
      role="button"
      tabindex="0"
      @click="goSearch"
      @keyup.enter="goSearch"
    >
      <span class="search-icon">🔍</span>
      <span class="search-placeholder">搜索想吃的美食</span>
    </div>

    <!-- 频道分类 + 附近店铺：搜索框触顶后作为一个整体滚动 -->
    <div ref="browseBlock" class="browse-block">
      <section class="channel">
        <div v-for="item in CATEGORIES" :key="item.name" class="channel-chip">
          <span class="chip-emoji">{{ item.emoji }}</span>
          <span>{{ item.name }}</span>
        </div>
      </section>

      <section class="shop-feed">
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

        <p v-if="feedLoading" class="feed-loading">加载中…</p>
      </section>
    </div>

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
  position: sticky;
  top: var(--brand-h, 3.6rem); /* 吸在品牌栏下方（动态高度） */
  z-index: 4;
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

/* 分类 + 店铺：搜索框触顶后作为整体滚动 */
.browse-block {
  /* 高度由 measureShell() 测量后通过 --browse-h 注入：正好等于吸顶搜索框
     与底部 Tab 之间的空间，避免列表底部被 Tab 挡住 */
  height: var(--browse-h, calc(100vh - 14rem));
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.search-box:focus-within {
  border-color: #ff5000;
  box-shadow: 0 0 0 3px rgba(255, 80, 0, 0.12);
}
.search-icon {
  font-size: 1rem;
  line-height: 1;
}
.search-placeholder {
  flex: 1;
  font-size: 0.95rem;
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
  gap: 0.9rem;
  padding-right: 0.25rem;
}
.shop-feed {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.feed-loading {
  text-align: center;
  color: #b0b0b0;
  font-size: 0.8rem;
  margin: 0;
}
.shop-card {
  display: flex;
  align-items: center;
  gap: 1.15rem;
  background: #fff;
  border-radius: var(--card-radius);
  padding: 1.4rem 1.3rem;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  text-decoration: none;
  color: inherit;
  transition: box-shadow 0.12s ease;
}
.shop-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}
.shop-thumb {
  flex-shrink: 0;
  width: 5.4rem;
  height: 5.4rem;
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
@media (max-width: 560px) {
  .channel {
    grid-template-columns: repeat(4, 1fr);
    row-gap: 0.6rem;
  }
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }
  .hero-emoji {
    font-size: 1.6rem;
  }
  .deal-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .flash-home {
    padding: 0.5rem;
  }
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
