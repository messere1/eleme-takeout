<script setup>

import { onMounted, ref } from 'vue'
import { changeStatus, createCategory, deleteCategory, getShop, listCategories, updateShop } from '@/api/shop'
import { session } from '@/utils/session'

const STATUS_TEXT = {
  OPEN: '营业中',
  CLOSED: '休息中',
  TEMP_CLOSED: '临时闭店',
}

const stored = session.loadShop()
const shopId = stored?.shopId
const missingShop = !shopId

const shop = ref(null)
const editName = ref('')
const editNotice = ref('')
const categories = ref([])
const newCategoryName = ref('')
const newCategorySort = ref('')
const message = ref('')

async function load() {
  if (missingShop) return
  shop.value = await getShop(shopId)
  editName.value = shop.value.shopName || ''
  editNotice.value = shop.value.notice || ''
  categories.value = await listCategories(shopId)
}

async function saveShop() {
  message.value = ''
  try {
    const data = await updateShop(shopId, {
      shopName: editName.value.trim(),
      notice: editNotice.value.trim(),
    })
    shop.value.shopName = data.shopName
    message.value = '已保存'
  } catch (error) {
    message.value = error?.message || '保存失败，请稍后重试'
  }
}

async function onStatusChange() {
  message.value = ''
  try {
    const data = await changeStatus(shopId, shop.value.status)
    shop.value.status = data.status
    message.value = '营业状态已更新'
  } catch (error) {
    message.value = error?.message || '更新失败，请稍后重试'
  }
}

async function addCategory() {
  message.value = ''
  const name = newCategoryName.value.trim()
  if (!name) {
    message.value = '请输入分类名称'
    return
  }
  try {
    const sort = Number(newCategorySort.value)
    const created = await createCategory(shopId, { name, sort: Number.isNaN(sort) ? 0 : sort })
    categories.value.push(created)
    newCategoryName.value = ''
    newCategorySort.value = ''
  } catch (error) {
    message.value = error?.message || '添加失败，请稍后重试'
  }
}

async function removeCategory(category) {
  message.value = ''
  try {
    await deleteCategory(category.id)
    categories.value = categories.value.filter((entry) => entry.id !== category.id)
  } catch (error) {
    message.value = error?.message || '删除失败（分类下存在商品时无法删除）'
  }
}

onMounted(load)
</script>

<template>
  <section class="console">
    <div class="console-top">
      <h2>商家后台</h2>
      <nav class="console-nav">
        <RouterLink to="/merchant">店铺</RouterLink>
        <RouterLink to="/merchant/products">商品管理</RouterLink>
        <RouterLink to="/merchant/orders">订单管理</RouterLink>
      </nav>
    </div>

    <p v-if="missingShop" class="console-missing">
      还没有店铺？<RouterLink to="/register">去注册开店</RouterLink>，或
      <RouterLink to="/login">用账号登录</RouterLink>。
    </p>

    <template v-else-if="shop">
      <section class="panel">
        <h3>店铺</h3>
        <div class="console-head">
          <strong data-testid="console-shop-name">{{ shop.shopName }}</strong>
          <span
            data-testid="console-shop-status"
            class="status-pill"
            :class="{ open: shop.status === 'OPEN' }"
          >{{ STATUS_TEXT[shop.status] || shop.status }}</span>
        </div>

        <label class="ctrl-label" for="console-name">店名</label>
        <el-input
          id="console-name"
          v-model="editName"
          data-testid="console-name-input"
          maxlength="50"
        />
        <label class="ctrl-label" for="console-notice">公告</label>
        <el-input
          id="console-notice"
          v-model="editNotice"
          data-testid="console-notice-input"
          maxlength="255"
        />

        <div class="row">
          <label class="ctrl-label" for="console-status">营业状态</label>
          <select
            id="console-status"
            v-model="shop.status"
            data-testid="console-status"
            class="select"
            @change="onStatusChange"
          >
            <option value="OPEN">营业中</option>
            <option value="CLOSED">休息中</option>
            <option value="TEMP_CLOSED">临时闭店</option>
          </select>
        </div>

        <button class="primary-btn" data-testid="console-save-shop" @click="saveShop">保存店铺资料</button>
      </section>

      <section class="panel">
        <h3>商品分类</h3>
        <ul v-if="categories.length" class="cat-list">
          <li
            v-for="category in categories"
            :key="category.id"
            :data-testid="`category-item-${category.id}`"
            class="cat-item"
          >
            <span>{{ category.name }}（排序 {{ category.sort }}）</span>
            <button
              class="link-danger"
              :data-testid="`category-delete-${category.id}`"
              @click="removeCategory(category)"
            >删除</button>
          </li>
        </ul>
        <p v-else class="empty-tip">还没有分类，先添加一个</p>

        <div class="cat-form">
          <el-input
            v-model="newCategoryName"
            data-testid="category-name-input"
            class="cat-name"
            placeholder="分类名称"
            maxlength="30"
          />
          <el-input
            v-model="newCategorySort"
            data-testid="category-sort-input"
            class="cat-sort"
            placeholder="排序"
          />
          <button class="primary-btn" data-testid="category-create-btn" @click="addCategory">添加</button>
        </div>
      </section>

      <p v-if="message" data-testid="console-message" role="status" class="console-message">
        {{ message }}
      </p>
    </template>
  </section>
</template>

<style scoped>
.console {
  max-width: 44rem;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.console h2 {
  margin: 0;
}
.console-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}
.console-nav a {
  margin-left: 0.6rem;
  color: var(--el-color-primary);
  font-weight: 600;
  text-decoration: none;
}
.panel {
  background: #fff;
  border-radius: var(--card-radius);
  padding: 1.25rem 1.5rem;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
.panel h3 {
  margin: 0 0 0.25rem;
}
.console-head {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.console-head strong {
  font-size: 1.15rem;
}
.status-pill {
  font-size: 0.85rem;
  background: #f2f3f5;
  color: #666;
  border-radius: 999px;
  padding: 0.15rem 0.7rem;
}
.status-pill.open {
  background: #fff4ea;
  color: #ff6a00;
}
.ctrl-label {
  font-size: 0.85rem;
  color: #666;
}
.row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.select {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 0.5rem 0.75rem;
  background: #fff;
  color: #2b2b2b;
}
.primary-btn {
  border: none;
  background: var(--brand-gradient);
  color: #2b1d00;
  font-weight: 600;
  border-radius: 999px;
  padding: 0.5rem 1.3rem;
  cursor: pointer;
  align-self: flex-start;
}
.cat-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.cat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 0.6rem 0.9rem;
}
.link-danger {
  border: none;
  background: transparent;
  color: #e34d1c;
  cursor: pointer;
}
.cat-form {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.5rem;
  align-items: center;
}
.cat-name {
  flex: 1;
}
.cat-sort {
  width: 5.5rem;
}
.empty-tip {
  color: #aaa;
}
.console-message {
  margin: 0;
  color: #ff6a00;
  font-weight: 600;
}
.console-missing {
  background: #fff;
  border-radius: var(--card-radius);
  padding: 1.5rem;
  text-align: center;
}
.console-missing a {
  color: #ff6a00;
}
</style>
