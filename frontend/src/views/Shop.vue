<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getShop, listCategories, listProducts } from '@/api/shop'
import { addToCart } from '@/api/cart'

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

function fmt(value) {
  return (Number(value) || 0).toFixed(2)
}

async function selectCategory(categoryId) {
  activeCategoryId.value = categoryId
  const data = await listProducts(categoryId, {
    page: 1,
    size: 20,
  })
  products.value = data?.items ?? []
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
  try {
    await load()
  } catch {
    loadError.value = '未连接后端，无法加载店铺（仅静态预览）'
  }
})
</script>

<template>
  <section class="shop-page">
    <template v-if="shop">
      <header class="shop-head">
        <div>
          <h2 data-testid="shop-name">{{ shop.shopName }}</h2>
          <p data-testid="shop-notice" class="shop-notice">{{ shop.notice }}</p>
        </div>
        <span
          data-testid="shop-status"
          class="shop-status"
          :class="{ open: shop.status === 'OPEN' }"
        >{{ STATUS_TEXT[shop.status] || shop.status }}</span>
      </header>

      <nav class="cat-bar">
        <button
          v-for="category in categories"
          :key="category.id"
          :data-testid="`category-${category.id}`"
          class="cat-tab"
          :class="{ active: activeCategoryId === category.id }"
          @click="selectCategory(category.id)"
        >
          {{ category.name }}
        </button>
      </nav>

      <ul v-if="products.length" class="product-list">
        <li
          v-for="product in products"
          :key="product.id"
          :data-testid="`product-card-${product.id}`"
          class="product-card"
        >
          <div class="product-info">
            <strong>{{ product.name }}</strong>
            <span class="product-desc">{{ product.description }}</span>
            <span class="product-price">¥{{ fmt(product.price) }}</span>
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

      <p v-if="message" data-testid="shop-message" role="status" class="shop-message">
        {{ message }}
      </p>
    </template>
    <p v-else class="loading-tip">{{ loadError || '店铺加载中…' }}</p>
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
.shop-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}
.shop-head h2 {
  margin: 0;
}
.shop-notice {
  margin: 0.35rem 0 0;
  color: #888;
  font-size: 0.9rem;
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
</style>
