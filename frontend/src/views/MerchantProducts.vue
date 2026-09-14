<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  changeProductStatus,
  createProduct,
  deleteProduct,
  getMyShop,
  listCategories,
  listMerchantProducts,
  updateProduct,
  updateProductPrice,
  updateProductStock,
} from '@/api/shop'
import ImageUploader from '@/components/ImageUploader.vue'
import { isPositiveMoney } from '@/utils/money'

// 店铺一律以服务端为准：登录接口不返回 shopId，本地缓存（takeout-shop）只在注册时写过，
// 退出登录或任意 401 都会清掉它，靠缓存就会让商家退出再登录后进不了商品管理。
const shopId = ref(null)
const missingShop = ref(false)
const loadingShop = ref(true)

const STATUS_TEXT = { ON_SALE: '在售', OFF_SALE: '已下架' }

const products = ref([])
const categories = ref([])
const stockEdit = reactive({})
const priceEdit = reactive({})
const editingNameId = ref(null)
const nameEdit = reactive({})
const message = ref('')
const creating = ref(false)
// 上传成功后写入这里；预览优先取它，其次回退到已保存的 product.imageUrl，
// 这样刷新后已上传的菜品图不会消失。
const imgSrc = reactive({})

const productPage = ref(1)
const productTotalPages = ref(1)
const form = reactive({ categoryId: '', name: '', price: '', stock: '' })

function fmt(value) {
  return (Number(value) || 0).toFixed(2)
}

async function load(page = 1) {
  try {
    const mine = await getMyShop()
    shopId.value = mine?.id ? Number(mine.id) : null
  } catch {
    // 拿不到「我的店铺」等价于这个账号没有绑定店铺，下面统一走注册/登录提示。
    shopId.value = null
  }
  try {
    if (!shopId.value) {
      missingShop.value = true
      return
    }
    missingShop.value = false
    const [cats, res] = await Promise.all([
      listCategories(shopId.value),
      listMerchantProducts({ page, size: 20 }),
    ])
    categories.value = cats || []
    if (!form.categoryId && categories.value.length) {
      form.categoryId = String(categories.value[0].id)
    }
    const items = Array.isArray(res) ? res : res?.items || []
    products.value = items
    productPage.value = Array.isArray(res) ? 1 : res?.page ?? page
    productTotalPages.value = Array.isArray(res) ? 1 : res?.totalPages ?? 1
    products.value.forEach((product) => {
      stockEdit[product.id] = product.stock
      priceEdit[product.id] = product.price
      nameEdit[product.id] = product.name
    })
  } catch (error) {
    message.value = error?.message || '商品加载失败，请稍后重试'
  } finally {
    loadingShop.value = false
  }
}

async function saveProduct(product) {
  message.value = ''
  const name = (nameEdit[product.id] || '').trim()
  if (!name) {
    message.value = '请输入商品名称'
    return
  }
  try {
    const res = await updateProduct(product.id, {
      name,
      categoryId: product.categoryId,
      description: product.description || '',
      price: Number(product.price),
      stock: Number(product.stock),
    })
    product.name = res?.name ?? name
    editingNameId.value = null
  } catch (error) {
    message.value = error?.message || '保存失败，请稍后重试'
  }
}

async function savePrice(product) {
  message.value = ''
  // FR-010 / EX-029：价格必须严格校验，不接受指数形式、三位小数，也不得先舍入再接受。
  // Number() 会放过 1.234、1e-7 这类写法，所以先按 §9.1 的金额格式判一次。
  const raw = String(priceEdit[product.id] ?? '').trim()
  if (!isPositiveMoney(raw)) {
    message.value = '价格需为 0.01～99999999.99 且最多两位小数'
    return
  }
  const price = Number(raw)
  try {
    const res = await updateProductPrice(product.id, price)
    product.price = res?.price ?? price
  } catch (error) {
    message.value = error?.message || '改价失败，请稍后重试'
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
  // FR-010 / EX-029：同上，先按金额格式判，不靠 Number() 的隐式转换。
  if (!isPositiveMoney(form.price)) {
    message.value = '价格需为 0.01～99999999.99 且最多两位小数'
    return
  }
  const price = Number(String(form.price).trim())
  if (!Number.isInteger(stock) || stock < 0) {
    message.value = '库存需为非负整数'
    return
  }
  creating.value = true
  try {
    const created = await createProduct(shopId.value, {
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
    <p v-if="loadingShop">店铺加载中…</p>
    <p v-else-if="missingShop" class="console-missing">
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
            <ImageUploader
              :model-value="imgSrc[product.id] ?? product.imageUrl ?? ''"
              target-type="PRODUCT_IMAGE"
              :target-id="product.id"
              shape="square"
              plain
              placeholder="菜品图"
              upload-label="上传图片"
              replace-label="换图"
              :input-testid="`product-img-${product.id}`"
              @update:model-value="imgSrc[product.id] = $event"
              @message="message = $event"
            />
            <template v-if="editingNameId === product.id">
              <el-input
                v-model="nameEdit[product.id]"
                class="name-input"
                placeholder="商品名称"
                maxlength="50"
              />
              <button class="link-btn" @click="saveProduct(product)">保存</button>
              <button class="link-btn" @click="editingNameId = null">取消</button>
            </template>
            <strong v-else>{{ product.name }}</strong>
            <span class="mgmt-price">¥{{ fmt(product.price) }}</span>
            <span class="mgmt-status" :class="{ off: product.status !== 'ON_SALE' }">
              {{ STATUS_TEXT[product.status] || product.status }}
            </span>
            <span class="mgmt-stock-show">库存 {{ product.stock }}</span>
          </div>

          <div class="mgmt-actions">
            <button
              class="link-btn"
              :data-testid="`product-mgmt-edit-${product.id}`"
              @click="editingNameId = product.id"
            >改名</button>
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

            <el-input
              v-model="priceEdit[product.id]"
              class="price-input"
              :data-testid="`product-mgmt-price-${product.id}`"
              placeholder="价格"
            />
            <button
              class="link-btn"
              :data-testid="`product-mgmt-save-price-${product.id}`"
              @click="savePrice(product)"
            >改价</button>

            <button
              class="link-danger"
              :data-testid="`product-mgmt-delete-${product.id}`"
              @click="removeProduct(product)"
            >删除</button>
          </div>
        </li>
      </ul>
      <p v-else class="empty-tip">还没有商品，先添加一个</p>

      <div v-if="productTotalPages > 1" class="product-pager">
        <button
          class="page-btn"
          data-testid="products-prev"
          :disabled="productPage <= 1"
          @click="load(productPage - 1)"
        >上一页</button>
        <span class="page-info">第 {{ productPage }} / {{ productTotalPages }} 页</span>
        <button
          class="page-btn"
          data-testid="products-next"
          :disabled="productPage >= productTotalPages"
          @click="load(productPage + 1)"
        >下一页</button>
      </div>

      <div class="create-panel">
        <h3>新增商品</h3>
        <label class="ctrl-label" for="product-category">所属分类</label>
        <div class="create-row">
          <select v-model="form.categoryId" data-testid="product-category" class="select">
            <option v-if="!categories.length" value="" disabled>（暂无分类，请先到「店铺」添加）</option>
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
.price-input {
  width: 5rem;
}
.name-input {
  width: 11rem;
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
.ctrl-label {
  font-size: 0.9rem;
  color: #666;
}
</style>
