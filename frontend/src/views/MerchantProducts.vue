<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  changeProductStatus,
  createProduct,
  deleteProduct,
  listCategories,
  listMerchantProducts,
  updateProductStock,
} from '@/api/shop'
import { session } from '@/utils/session'

const stored = session.loadShop()
const shopId = stored?.shopId
const missingShop = !shopId

const STATUS_TEXT = { ON_SALE: '在售', OFF_SALE: '已下架' }

const products = ref([])
const categories = ref([])
const stockEdit = reactive({})
const message = ref('')
const creating = ref(false)
const form = reactive({ categoryId: '', name: '', price: '', stock: '' })

function fmt(value) {
  return (Number(value) || 0).toFixed(2)
}

async function load() {
  if (missingShop) return
  try {
    const [cats, list] = await Promise.all([
      listCategories(shopId),
      listMerchantProducts(shopId),
    ])
    categories.value = cats || []
    products.value = list || []
    products.value.forEach((product) => {
      stockEdit[product.id] = product.stock
    })
  } catch (error) {
    message.value = error?.message || '商品加载失败，请稍后重试'
  }
}

async function toggleStatus(product) {
  message.value = ''
  const target = product.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE'
  try {
    const res = await changeProductStatus(product.id, target)
    product.status = res?.status || target
  } catch (error) {
    message.value = error?.message || '操作失败，请稍后重试'
  }
}

async function saveStock(product) {
  message.value = ''
  const stock = Number(stockEdit[product.id])
  if (!Number.isInteger(stock) || stock < 0) {
    message.value = '库存需为非负整数'
    return
  }
  try {
    const res = await updateProductStock(product.id, stock)
    product.stock = res?.stock ?? stock
  } catch (error) {
    message.value = error?.message || '保存失败，请稍后重试'
  }
}

async function removeProduct(product) {
  message.value = ''
  try {
    await deleteProduct(product.id)
    products.value = products.value.filter((entry) => entry.id !== product.id)
  } catch (error) {
    message.value = error?.message || '删除失败，请稍后重试'
  }
}

async function createNew() {
  message.value = ''
  const name = form.name.trim()
  const price = Number(form.price)
  const stock = Number(form.stock)
  const categoryId = Number(form.categoryId)
  if (!form.categoryId) {
    message.value = '请选择分类'
    return
  }
  if (!name) {
    message.value = '请输入商品名称'
    return
  }
  if (!Number.isFinite(price) || price <= 0) {
    message.value = '价格需大于 0'
    return
  }
  if (!Number.isInteger(stock) || stock < 0) {
    message.value = '库存需为非负整数'
    return
  }
  creating.value = true
  try {
    const created = await createProduct(shopId, {
      categoryId,
      name,
      description: '',
      price,
      stock,
    })
    products.value.push(created)
    stockEdit[created.id] = created.stock
    form.name = ''
    form.price = ''
    form.stock = ''
  } catch (error) {
    message.value = error?.message || '创建失败，请稍后重试'
  } finally {
    creating.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="console">
    <h2>商品管理</h2>
    <p v-if="missingShop" class="console-missing">
      还没有店铺？<RouterLink to="/register">去注册开店</RouterLink>，或
      <RouterLink to="/login">用账号登录</RouterLink>。
    </p>

    <template v-else>
      <p v-if="message" data-testid="product-message" class="console-message">{{ message }}</p>

      <ul v-if="products.length" class="mgmt-list">
        <li
          v-for="product in products"
          :key="product.id"
          :data-testid="`product-mgmt-item-${product.id}`"
          class="mgmt-item"
        >
          <div class="mgmt-main">
            <strong>{{ product.name }}</strong>
            <span class="mgmt-price">¥{{ fmt(product.price) }}</span>
            <span class="mgmt-status" :class="{ off: product.status !== 'ON_SALE' }">
              {{ STATUS_TEXT[product.status] || product.status }}
            </span>
            <span class="mgmt-stock-show">库存 {{ product.stock }}</span>
          </div>

          <div class="mgmt-actions">
            <button
              class="primary-btn small"
              :data-testid="`product-mgmt-status-${product.id}`"
              @click="toggleStatus(product)"
            >{{ product.status === 'ON_SALE' ? '下架' : '上架' }}</button>

            <el-input
              v-model="stockEdit[product.id]"
              class="stock-input"
              :data-testid="`product-mgmt-stock-${product.id}`"
              placeholder="库存"
            />
            <button
              class="link-btn"
              :data-testid="`product-mgmt-save-stock-${product.id}`"
              @click="saveStock(product)"
            >保存</button>
            <button
              class="link-danger"
              :data-testid="`product-mgmt-delete-${product.id}`"
              @click="removeProduct(product)"
            >删除</button>
          </div>
        </li>
      </ul>
      <p v-else class="empty-tip">还没有商品，先添加一个</p>

      <div class="create-panel">
        <h3>新增商品</h3>
        <div class="create-row">
          <select v-model="form.categoryId" data-testid="product-category" class="select">
            <option value="">选择分类</option>
            <option v-for="category in categories" :key="category.id" :value="String(category.id)">
              {{ category.name }}
            </option>
          </select>
          <el-input v-model="form.name" data-testid="product-name" placeholder="商品名称" maxlength="50" />
          <el-input v-model="form.price" data-testid="product-price" placeholder="价格" />
          <el-input v-model="form.stock" data-testid="product-stock" placeholder="库存" />
          <button
            class="primary-btn"
            data-testid="product-create-btn"
            :disabled="creating"
            @click="createNew"
          >{{ creating ? '添加中…' : '添加' }}</button>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.console {
  max-width: 52rem;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.console h2 {
  margin: 0;
}
.console-missing {
  background: #fff;
  border-radius: var(--card-radius);
  padding: 1.5rem;
  text-align: center;
}
.console-message {
  margin: 0;
  color: #ff6a00;
  font-weight: 600;
}
.mgmt-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.mgmt-item {
  background: #fff;
  border-radius: var(--card-radius);
  padding: 0.8rem 1rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}
.mgmt-main {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}
.mgmt-price {
  color: #ff2f00;
  font-weight: 700;
}
.mgmt-status {
  font-size: 0.78rem;
  background: #e7f8ee;
  color: #17a25c;
  border-radius: 999px;
  padding: 0.1rem 0.5rem;
}
.mgmt-status.off {
  background: #f2f3f5;
  color: #888;
}
.mgmt-stock-show {
  color: #999;
  font-size: 0.85rem;
}
.mgmt-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.stock-input {
  width: 5rem;
}
.stock-input :deep(.el-input) {
  width: 100%;
}
.primary-btn {
  border: none;
  background: var(--brand-gradient);
  color: #2b1d00;
  font-weight: 600;
  border-radius: 999px;
  padding: 0.45rem 1.1rem;
  cursor: pointer;
}
.primary-btn.small {
  padding: 0.3rem 0.9rem;
  font-size: 0.85rem;
}
.link-btn {
  border: none;
  background: transparent;
  color: var(--el-color-primary);
  cursor: pointer;
}
.link-danger {
  border: none;
  background: transparent;
  color: #e34d1c;
  cursor: pointer;
}
.empty-tip {
  color: #aaa;
  text-align: center;
}
.create-panel {
  background: #fff;
  border-radius: var(--card-radius);
  padding: 1rem 1.2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.create-panel h3 {
  margin: 0 0 0.6rem;
}
.create-row {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
}
.select {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 0.5rem 0.6rem;
  background: #fff;
  color: #333;
}
</style>
