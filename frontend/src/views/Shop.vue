<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getShop, listCategories, listProducts } from '@/api/shop'
import { addToCart, getCart, saveDeliveryInfo } from '@/api/cart'
import { createOrder } from '@/api/order'
import { getProfile } from '@/api/user'
import { session } from '@/utils/session'
import { isValidRecipientPhone, normalizeRecipientPhone } from '@/utils/recipientPhone'

const route = useRoute()
const router = useRouter()
const shopId = Number(route.params.id)

const shop = ref(null)
const categories = ref([])
const products = ref([])
const activeCategoryId = ref(null)
const message = ref('')
const loadError = ref('')
const pendingAddId = ref(null)

const STATUS_TEXT = {
  OPEN: '营业中',
  CLOSED: '休息中',
  TEMP_CLOSED: '临时打烊',
}

function goBack() {
  if (window.history.state?.back) {
    router.back()
    return
  }
  const { from, q } = route.query
  if (from === 'search') router.push({ path: '/search', query: q ? { q: String(q) } : {} })
  else router.push('/')
}

function fmt(value) {
  return (Number(value) || 0).toFixed(2)
}

// 后端把营业时间序列化成 "HH:mm" 或 "HH:mm:ss"，只取前五位显示。
function fmtClock(value) {
  return value ? String(value).slice(0, 5) : ''
}

const FOOD_EMOJI = ['🍜', '🍔', '🧋', '🍕', '🍣', '🥟', '🍗', '🍤', '🍰', '🥡']
const FOOD_BG = ['#fff1e8', '#fff0f0', '#f4f1ff', '#eef8ff', '#fff7ed', '#fdeef7']

function foodKey(name) {
  let h = 0
  const text = String(name || '')
  for (let i = 0; i < text.length; i += 1) h = (h * 31 + text.charCodeAt(i)) >>> 0
  return h
}

function foodEmoji(name) {
  return FOOD_EMOJI[foodKey(name) % FOOD_EMOJI.length]
}

function foodBg(name) {
  return FOOD_BG[foodKey(name) % FOOD_BG.length]
}

const productPage = ref(1)
const productTotalPages = ref(1)

const isCustomer = session.load()?.role === 'CUSTOMER'
// GET /cart 返回全站购物车（每条带 shopId），这里只取当前店铺的条目。
const cartItems = ref([])
const sheetOpen = ref(false)
const recipient = ref('')
const contact = ref('')
const deliveryAddress = ref('')
const ordering = ref(false)

const shopCartItems = computed(() =>
  cartItems.value.filter((item) => item.shopId === shopId),
)
// 合计口径与后端 CartService.get 一致：只累计可购买的行。
const shopCartTotal = computed(() =>
  shopCartItems.value
    .filter((item) => item.available)
    .reduce((sum, item) => sum + (Number(item.subtotal) || 0), 0),
)

function cartCount() {
  return shopCartItems.value.reduce((sum, item) => sum + (item.quantity || 0), 0)
}

async function loadCart() {
  if (!isCustomer) return
  try {
    const data = await getCart()
    cartItems.value = data?.items ?? []
  } catch {
    /* 未登录或接口不可用 */
  }
}

async function seedAddress() {
  if (!isCustomer) return
  try {
    const me = await getProfile()
    if (me?.address) deliveryAddress.value = me.address
    if (me?.username) recipient.value = me.username
    if (me?.phone) contact.value = me.phone
  } catch {
    /* 忽略 */
  }
}

async function openSheet() {
  sheetOpen.value = true
  await seedAddress()
  await loadCart()
}

// 与 Cart.vue 同一套收货字段校验；按钮不可提交时要说清是哪一项不满足。
const checkoutBlockedReason = computed(() => {
  if (!isCustomer) return '请先登录顾客账号再下单'
  if (!shopCartItems.value.length) return '本店购物车还是空的，先加购商品'
  if (shopCartItems.value.some((item) => !item.available)) return '购物车里有已下架或缺货的商品，请到购物车页移除'
  if (!recipient.value.trim()) return '请填写收货人'
  if (!isValidRecipientPhone(contact.value)) return '联系电话需为 7~15 位数字（可含 +、空格、连字符）'
  if (deliveryAddress.value.trim().length < 5) return '收货地址至少 5 个字符'
  return ''
})
const canCheckout = computed(() => !checkoutBlockedReason.value && !ordering.value)

async function submitFromSheet() {
  if (ordering.value) return
  if (checkoutBlockedReason.value) {
    message.value = checkoutBlockedReason.value
    return
  }
  ordering.value = true
  message.value = ''
  try {
    const delivery = {
      shopId,
      recipientName: recipient.value.trim(),
      recipientPhone: normalizeRecipientPhone(contact.value),
      deliveryAddress: deliveryAddress.value.trim(),
      saveToProfile: false,
    }
    await saveDeliveryInfo(delivery)
    const order = await createOrder(delivery)
    sheetOpen.value = false
    router.push(`/orders/${order.id}`)
  } catch (error) {
    message.value = error?.message || '下单失败，请稍后重试'
  } finally {
    ordering.value = false
  }
}

const prodLoading = ref(false)

async function selectCategory(categoryId, page = 1, append = false) {
  if (prodLoading.value) return
  prodLoading.value = true
  activeCategoryId.value = categoryId
  try {
    const data = await listProducts(categoryId, { page, size: 20 })
    const items = data?.items ?? []
    products.value = append ? products.value.concat(items) : items
    productPage.value = data?.page ?? page
    productTotalPages.value = data?.totalPages ?? 1
  } finally {
    prodLoading.value = false
  }
}

// 光标在商品框内滚动时触底加载下一页；框外滚动不受影响
function onProductsScroll(event) {
  const el = event.target
  const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 60
  if (nearBottom && productPage.value < productTotalPages.value) {
    selectCategory(activeCategoryId.value, productPage.value + 1, true)
  }
}

async function load() {
  shop.value = await getShop(shopId)
  categories.value = await listCategories(shopId)
  if (categories.value.length) {
    await selectCategory(categories.value[0].id)
  }
}

async function addProduct(product) {
  message.value = ''
  if (shop.value?.status !== 'OPEN') {
    message.value = '店铺未营业，暂不能加购'
    return
  }
  pendingAddId.value = product.id
  try {
    await addToCart({ productId: product.id, quantity: 1 })
    message.value = '已加入购物车'
    loadCart()
  } catch (error) {
    if (error?.code === 'AUTH_INVALID' || error?.code === 'AUTH_EXPIRED') {
      router.push('/login')
      return
    }
    message.value = error?.message || '加购失败，请稍后重试'
  } finally {
    pendingAddId.value = null
  }
}

onMounted(async () => {
  // 进店就按本店已有商品显示底部购物车，不必等用户再点一次加购。
  loadCart()
  try {
    await load()
  } catch {
    loadError.value = '未连接后端，无法加载店铺（仅静态预览）'
  }
})
</script>

<template>
  <section class="shop-page">
    <button class="back-home" data-testid="shop-back" @click="goBack">← 返回</button>
    <template v-if="shop">
      <img
        v-if="shop.coverImageUrl"
        :src="shop.coverImageUrl"
        class="shop-cover"
        alt="店铺封面"
        data-testid="shop-cover"
      />
      <header class="shop-head">
        <img
          v-if="shop.imageUrl"
          :src="shop.imageUrl"
          class="shop-logo"
          alt="店铺照片"
          data-testid="shop-image"
        />
        <div class="shop-info">
          <h2 data-testid="shop-name">{{ shop.shopName }}</h2>
          <p data-testid="shop-notice" class="shop-notice">{{ shop.notice }}</p>
          <p
            v-if="shop.openingTime && shop.closingTime"
            data-testid="shop-hours"
            class="shop-hours"
          >营业时间 {{ fmtClock(shop.openingTime) }}–{{ fmtClock(shop.closingTime) }}</p>
        </div>
        <span
          data-testid="shop-status"
          class="shop-status"
          :class="{ open: shop.status === 'OPEN' }"
        >{{ STATUS_TEXT[shop.status] || shop.status }}</span>
      </header>

      <div class="shop-body">
        <aside class="cat-rail">
          <button
            v-for="category in categories"
            :key="category.id"
            :data-testid="`category-${category.id}`"
            class="cat-tab rail-tab"
            :class="{ active: activeCategoryId === category.id }"
            @click="selectCategory(category.id)"
          >
            {{ category.name }}
          </button>
        </aside>

        <div class="prod-scroll" @scroll="onProductsScroll">
          <ul v-if="products.length" class="product-list">
            <li
              v-for="product in products"
              :key="product.id"
              :data-testid="`product-card-${product.id}`"
              class="product-card"
            >
              <img v-if="product.imageUrl" :src="product.imageUrl" class="dish-thumb" alt="菜品图" />
              <div
                v-else
                class="dish-thumb"
                :style="{ background: foodBg(product.name) }"
              >{{ foodEmoji(product.name) }}</div>
              <div class="product-info">
                <strong>{{ product.name }}</strong>
                <span class="product-desc">{{ product.description }}</span>
                <span class="product-price">¥{{ fmt(product.price) }}</span>
                <div class="product-tags">
                  <span class="tag">招牌</span>
                  <span class="tag">可加购</span>
                </div>
              </div>
              <button
                :data-testid="`add-${product.id}`"
                class="add-btn"
                :disabled="product.status !== 'ON_SALE' || shop.status !== 'OPEN' || pendingAddId === product.id"
                @click="addProduct(product)"
              >
                加购
              </button>
            </li>
          </ul>
          <p v-else class="empty-tip">该分类暂时没有商品</p>
          <p v-if="prodLoading" class="feed-loading">加载中…</p>
        </div>
      </div>

      <p v-if="message" data-testid="shop-message" role="status" class="shop-message">
        {{ message }}
      </p>
    </template>
    <p v-else class="loading-tip">{{ loadError || '店铺加载中…' }}</p>

    <!-- 点单页底部购物车 -->
    <div v-if="isCustomer && shopCartItems.length" data-testid="shop-cart-bar" class="shop-cart-bar" @click="openSheet">
      <span class="cart-summary">🛒 共 {{ cartCount() }} 件</span>
      <strong>合计 ¥{{ fmt(shopCartTotal) }}</strong>
      <span class="cart-go">{{ sheetOpen ? '收起 ▲' : '去结算' }}</span>
    </div>

    <div v-if="sheetOpen" class="shop-sheet">
      <header class="sheet-head">
        <h3>订单信息</h3>
        <button class="sheet-close" @click="sheetOpen = false">收起 ▲</button>
      </header>
      <ul class="sheet-items">
        <li v-for="item in shopCartItems" :key="item.id" :data-testid="`shop-sheet-item-${item.id}`">
          <span>{{ item.productName }} × {{ item.quantity }}</span>
          <strong>¥{{ fmt(item.subtotal) }}</strong>
        </li>
      </ul>
      <div class="field">
        <label>收货人</label>
        <el-input v-model="recipient" placeholder="收货人" maxlength="30" />
      </div>
      <div class="field">
        <label>联系电话</label>
        <el-input v-model="contact" placeholder="联系电话" maxlength="20" />
      </div>
      <div class="field">
        <label>收货地址</label>
        <el-input v-model="deliveryAddress" placeholder="收货地址" maxlength="255" />
      </div>
      <div class="sheet-total">合计 <strong>¥{{ fmt(shopCartTotal) }}</strong></div>
      <p
        v-if="checkoutBlockedReason"
        data-testid="shop-checkout-hint"
        class="shop-checkout-hint"
      >{{ checkoutBlockedReason }}</p>
      <button data-testid="shop-checkout" class="checkout-btn" :disabled="!canCheckout" @click="submitFromSheet">
        {{ ordering ? '提交中…' : '提交订单' }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.shop-page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background: #fff;
  border-radius: var(--card-radius);
  padding: 1.5rem;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.06);
}
.shop-cover {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 6;
  object-fit: cover;
  border-radius: var(--card-radius);
  background: #fff4ec;
}
.shop-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}
.shop-info {
  flex: 1 1 auto;
  min-width: 0;
}
.shop-logo {
  flex: 0 0 auto;
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 10px;
  object-fit: cover;
  background: #fff4ec;
}
.shop-head h2 {
  margin: 0;
}
.shop-notice {
  margin: 0.35rem 0 0;
  color: #888;
  font-size: 0.9rem;
}
.shop-hours {
  margin: 0.25rem 0 0;
  color: var(--el-color-primary);
  font-size: 0.85rem;
}
.shop-status {
  flex-shrink: 0;
  font-size: 0.85rem;
  padding: 0.25rem 0.7rem;
  border-radius: 999px;
  background: #f2f3f5;
  color: #666;
}
.shop-status.open {
  background: #fff4ea;
  color: #ff6a00;
}
.cat-bar {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.cat-tab {
  border: 1px solid #eee;
  background: #fff;
  padding: 0.35rem 1rem;
  border-radius: 999px;
  cursor: pointer;
  color: #444;
}
.cat-tab.active {
  background: var(--brand-gradient);
  color: #2b1d00;
  border-color: transparent;
  font-weight: 600;
}
.product-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.product-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.9rem 1rem;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
}
.product-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.product-desc {
  color: #999;
  font-size: 0.85rem;
}
.product-price {
  color: #ff6a00;
  font-weight: 700;
}
.add-btn {
  flex-shrink: 0;
  border: none;
  background: var(--brand-gradient);
  color: #2b1d00;
  font-weight: 600;
  padding: 0.4rem 1.1rem;
  border-radius: 999px;
  cursor: pointer;
}
.add-btn:disabled {
  background: #eee;
  color: #999;
  cursor: not-allowed;
}
.shop-message {
  margin: 0;
  color: #ff6a00;
  font-weight: 600;
}
.empty-tip,
.loading-tip {
  color: #aaa;
  text-align: center;
}
.product-pager {
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
.shop-body {
  display: flex;
  gap: 0.75rem;
  align-items: stretch;
}
.cat-rail {
  width: 6.4rem;
  flex-shrink: 0;
  height: 60vh;
  position: sticky;
  top: 0.75rem;
  overflow-y: auto;
  overscroll-behavior: contain;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  border-right: 1px solid #f0f0f0;
  padding-right: 0.25rem;
}
.rail-tab {
  width: 100%;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.prod-scroll {
  flex: 1;
  min-width: 0;
  height: 60vh;
  overflow-y: auto;
  overscroll-behavior: contain;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
.back-home {
  align-self: flex-start;
  border: none;
  background: transparent;
  color: #666;
  cursor: pointer;
  padding: 0;
  font-size: 0.9rem;
}
.back-home:hover {
  color: #ff6a00;
}
.shop-head {
  flex-wrap: wrap;
}
.product-card {
  flex-wrap: wrap;
  gap: 0.9rem;
  padding: 0.8rem;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.12s ease;
}
.product-card:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
}
.dish-thumb {
  flex-shrink: 0;
  /* 3.9rem × 1.5 */
  width: 5.85rem;
  height: 5.85rem;
  border-radius: 10px;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
}
.product-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.product-info strong {
  font-size: 1.02rem;
}
.product-price {
  color: #ff2f00;
  font-weight: 700;
  font-size: 1.1rem;
}
.product-tags {
  display: flex;
  gap: 0.4rem;
  margin-top: 0.15rem;
}
.tag {
  font-size: 0.72rem;
  color: #ff6a00;
  background: #fff4ec;
  border-radius: 4px;
  padding: 0.05rem 0.4rem;
}

/* 点单页底部购物车条 + 上滑卷轴 */
.shop-cart-bar {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  bottom: calc(var(--tabbar-height) + 0.4rem);
  box-sizing: border-box;
  width: min(96%, 46rem);
  background: #2b1d00;
  color: #ffe08a;
  border-radius: 999px;
  padding: 0.7rem 1.2rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.25);
  cursor: pointer;
  z-index: 25;
}
.cart-summary {
  font-weight: 600;
}
.cart-go {
  background: #ff5000;
  color: #fff;
  border-radius: 999px;
  padding: 0.3rem 1rem;
  font-weight: 700;
}
.shop-sheet {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  /* 用 Tab 栏的实际高度定位，不要写死 60px：全面屏上 Tab 栏还带安全区，
     写死会被盖住一截（见 theme.css 的 --tabbar-height）。 */
  bottom: var(--tabbar-height);
  /* 关键：默认 content-box 下 width:100% 还要再加 padding，手机上会横向溢出、右边被切掉 */
  box-sizing: border-box;
  width: min(100%, 48rem);
  /* 移动端地址栏伸缩会让 vh 跳动，dvh 更稳；不支持 dvh 的浏览器用上一行兜底 */
  max-height: min(66vh, 32rem);
  max-height: min(66dvh, 32rem);
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  background: #fff;
  border-radius: 18px 18px 0 0;
  padding: 1rem 1.2rem calc(1.2rem + env(safe-area-inset-bottom, 0px));
  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.14);
  z-index: 26;
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.shop-sheet .sheet-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.shop-sheet .sheet-head h3 {
  margin: 0;
}
.shop-sheet .sheet-close {
  border: none;
  background: transparent;
  color: var(--el-color-primary);
  cursor: pointer;
}
.shop-sheet .sheet-items {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.shop-sheet .sheet-items li {
  display: flex;
  justify-content: space-between;
  font-size: 0.92rem;
}
.shop-sheet .sheet-items li strong {
  color: #ff2f00;
}
.shop-sheet .field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.shop-sheet .field label {
  font-size: 0.85rem;
  color: #666;
}
.shop-sheet .sheet-total {
  display: flex;
  justify-content: space-between;
  border-top: 1px dashed #eee;
  padding-top: 0.6rem;
}
.shop-checkout-hint {
  margin: 0 0 0.4rem;
  color: #e34d1c;
  font-size: 0.85rem;
}
.shop-sheet .sheet-total strong {
  color: #ff2f00;
}
.shop-sheet .checkout-btn {
  border: none;
  background: var(--brand-gradient);
  color: #fff;
  font-weight: 700;
  border-radius: 999px;
  padding: 0.7rem 0;
  cursor: pointer;
}
.product-card .add-btn {
  margin-left: auto;
}
@media (max-width: 520px) {
  .shop-page {
    padding: 0.9rem;
  }
  .shop-cart-bar {
    width: 94%;
  }
  .cat-rail {
    width: 5rem;
  }
  .product-info strong {
    word-break: break-word;
  }
  .product-desc {
    white-space: normal;
  }
}
</style>
