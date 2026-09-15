<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listRecommendedShops, listShops } from '@/api/shop'
import { listBusinessCategories } from '@/api/merchant'

const router = useRouter()

function goSearch() {
  router.push('/search')
}

const EMOJI = ['🥞', '🌶️', '🧋', '🍣', '🍢', '🍞', '🍔', '🥡']
const BG = ['#fff1e8', '#fff0f0', '#f4f1ff', '#eef8ff', '#fff7ed', '#fdeef7']

const shops = ref([])
const feedError = ref('')

// 短标签只给不与其它品类重名的用；奶茶甜品/甜品烘焙、中式快餐/快餐便当这类
// 名字相近的品类保留全名，否则两个 chip 会显示成同一个词。
const CATEGORY_META = { 快餐便当:['🍜','快餐'],奶茶饮品:['🧋','奶茶'],小吃炸物:['🥡','小吃'],汉堡披萨:['🍔','汉堡'],日韩料理:['🍣','日料'],烧烤夜宵:['🍢','烧烤'],甜品烘焙:['🍰','甜品'],健康轻食:['🥗','轻食'],中式快餐:['🍚','中式快餐'],西式简餐:['🍝','西式简餐'],奶茶甜品:['🍮','奶茶甜品'],地方菜系:['🥘','地方菜系'] }
const categories = ref([])
const activeCategoryId = ref(null)

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
let latestLoadId = 0

// 推荐位。加载失败不能影响首页主流程，静默隐藏整块即可。
const recommended = ref([])

// 取 10 条：外壳宽度 48rem，4 条只有 490px 根本填不满，横向滚动就没意义了
async function loadRecommended() {
  try {
    const items = await listRecommendedShops(10)
    recommended.value = (items || []).map(decorate)
  } catch {
    recommended.value = []
  }
}

async function load(page, append = false, categoryId = activeCategoryId.value) {
  if (feedLoading.value && append) return
  const loadId = ++latestLoadId
  feedLoading.value = true
  feedError.value = ''
  try {
    const params = { page, size: 20 }
    if (categoryId) params.businessCategoryId = categoryId
    const data = await listShops(params)
    if (loadId !== latestLoadId) return
    const items = (data?.items || []).map(decorate)
    shops.value = append ? shops.value.concat(items) : items
    currentPage.value = data?.page ?? page
    totalPages.value = data?.totalPages ?? 1
    if (!append && items.length === 0) feedError.value = '暂时没有可展示的店铺'
  } catch {
    if (loadId !== latestLoadId) return
    if (!append) {
      shops.value = []
      currentPage.value = 1
      totalPages.value = 1
      feedError.value = '店铺列表加载失败，请稍后重试'
    }
  } finally {
    if (loadId === latestLoadId) feedLoading.value = false
  }
}

async function selectCategory(id) { activeCategoryId.value = activeCategoryId.value === id ? null : id; await load(1,false,activeCategoryId.value) }

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

// 鼠标在品类条上滚动时转成横向滚动；滚到两端就把这次滚动交回页面。
// 必须 stopPropagation：否则 window 上的 onWindowWheel 还会再滚一次店铺区。
function onChannelWheel(event) {
  const el = event.currentTarget
  if (el.scrollWidth <= el.clientWidth) return
  const delta = event.deltaY
  const canRight = el.scrollLeft + el.clientWidth < el.scrollWidth - 1
  const canLeft = el.scrollLeft > 0
  if ((delta > 0 && !canRight) || (delta < 0 && !canLeft)) return
  el.scrollLeft += delta
  event.preventDefault()
  event.stopPropagation()
}

onMounted(() => {
  measureShell()
  load(1)
  loadRecommended()
  listBusinessCategories().then(items=>{categories.value=(items||[]).map(c=>{const meta=CATEGORY_META[c.name]||['🍽️',c.name.slice(0,4)];return {...c,emoji:meta[0],displayName:meta[1]}})}).catch(()=>{categories.value=[]})
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
      <section class="channel" data-testid="home-channel" @wheel="onChannelWheel">
        <button v-for="item in categories" :key="item.id" class="channel-chip" :class="{ active: activeCategoryId === item.id }" :data-testid="`home-category-${item.id}`" @click="selectCategory(item.id)">
          <span class="chip-emoji">{{ item.emoji }}</span>
          <span>{{ item.displayName }}</span>
        </button>
      </section>

      <section v-if="recommended.length" class="recommend" data-testid="home-recommend">
        <h2 class="recommend-title">猜你喜欢</h2>
        <div class="recommend-row" @wheel="onChannelWheel">
          <RouterLink
            v-for="shop in recommended"
            :key="shop.id"
            :to="`/shops/${shop.id}`"
            :data-testid="`home-recommend-${shop.id}`"
            class="recommend-card"
          >
            <div class="recommend-thumb" :style="{ background: shop.bg }">{{ shop.emoji }}</div>
            <strong class="recommend-name">{{ shop.shopName }}</strong>
            <span class="recommend-status" :class="{ open: shop.status === 'OPEN' }">{{ statusText(shop.status) }}</span>
          </RouterLink>
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
            <img
              v-if="shop.imageUrl"
              :src="shop.imageUrl"
              class="shop-thumb"
              alt="店铺照片"
              :data-testid="`home-shop-image-${shop.id}`"
            />
            <div v-else class="shop-thumb" :style="{ background: shop.bg }">{{ shop.emoji }}</div>
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
  display: flex;
  gap: 0.5rem;
  background: #fff;
  border-radius: var(--card-radius);
  padding: 0.9rem 0.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  /* 只横向滚动。browse-block 是固定高的纵向 flex 容器，不给 flex 就会被压矮，
     而 overflow-x 会把 overflow-y 隐式变成 auto，于是内部又冒出纵向滚动条、图标被滚掉。 */
  flex: 0 0 auto;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
}
.channel::-webkit-scrollbar {
  height: 6px;
}
.channel::-webkit-scrollbar-thumb {
  background: #e4e4e4;
  border-radius: 3px;
}
.channel-chip {
  /* 单行不换行，宽度够放下 emoji 和最长四字标签（奶茶甜品） */
  flex: 0 0 auto;
  min-width: 4.9rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.78rem;
  color: #444;
  border: 0;
  border-radius: 10px;
  padding: 0.45rem 0.5rem;
  background: transparent;
  cursor: pointer;
  white-space: nowrap;
}
.channel-chip.active {
  color: #ff5000;
  background: #fff1e8;
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

/* 推荐位：横向一条，不占纵向空间 */
.recommend {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  /* 同 .channel：browse-block 是固定高的纵向 flex 容器，不给 flex 会被压矮 */
  flex: 0 0 auto;
}
.recommend-title {
  margin: 0;
  font-size: 0.95rem;
  color: #333;
}
.recommend-row {
  display: flex;
  gap: 0.6rem;
  /* 同 .channel：overflow-x 会把 overflow-y 隐式变成 auto，必须显式关掉 */
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 0.25rem;
  scrollbar-width: thin;
}
.recommend-card {
  flex: 0 0 auto;
  width: 7.2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  padding: 0.6rem 0.4rem;
  border-radius: 0.9rem;
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  text-decoration: none;
  color: inherit;
}
.recommend-thumb {
  width: 2.6rem;
  height: 2.6rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.3rem;
}
.recommend-name {
  font-size: 0.78rem;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.recommend-status {
  font-size: 0.68rem;
  color: #b0b0b0;
}
.recommend-status.open {
  color: #21b36b;
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
  /* 5.4rem × 1.5 */
  width: 8.1rem;
  height: 8.1rem;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
  /* 有店铺照片时按图片渲染，object-fit 让它填满且不变形 */
  object-fit: cover;
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
    padding: 0.8rem 0.4rem;
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
