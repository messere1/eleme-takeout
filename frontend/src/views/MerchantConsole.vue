<script setup>

import { onMounted, reactive, ref } from 'vue'
import {
  changeStatus,
  createCategory,
  deleteCategory,
  getMyShop,
  getShop,
  listCategories,
  updateCategory,
  updateShop,
  updateBusinessHours,
} from '@/api/shop'
import ImageUploader from '@/components/ImageUploader.vue'
import { session } from '@/utils/session'

const STATUS_TEXT = {
  OPEN: '营业中',
  CLOSED: '休息中',
  TEMP_CLOSED: '临时闭店',
}

const stored = session.loadShop()
// 用 ref 而不是普通变量：它要作为 ImageUploader 的 targetId 绑定，必须在拿到后能触发更新
const shopId = ref(stored?.shopId ?? null)
const missingShop = ref(!shopId.value)

const shop = ref(null)
const cover = ref('')

const editName = ref('')
const editNotice = ref('')
const categories = ref([])
const newCategoryName = ref('')
const newCategorySort = ref('')
const editingCatId = ref(null)
const catNameEdit = reactive({})
const catSortEdit = reactive({})
const message = ref('')
const savingShop = ref(false)
const openingTime = ref('')
const closingTime = ref('')
const savingBusinessHours = ref(false)

function normalizeTime(value) { return value ? String(value).slice(0, 5) : '' }

function startEditCategory(category) {
  editingCatId.value = category.id
  catNameEdit[category.id] = category.name
  catSortEdit[category.id] = category.sort
}

async function saveCategory(category) {
  message.value = ''
  const name = (catNameEdit[category.id] || '').trim()
  if (!name) {
    message.value = '请输入分类名称'
    return
  }
  try {
    const sort = Number(catSortEdit[category.id])
    const res = await updateCategory(category.id, {
      name,
      sort: Number.isNaN(sort) ? category.sort : sort,
    })
    category.name = res?.name ?? name
    category.sort = res?.sort ?? category.sort
    editingCatId.value = null
  } catch (error) {
    message.value = error?.message || '保存失败，请稍后重试'
  }
}

async function load() {
  // 优先按登录身份获取“我的店铺”，接口不可用时回退本地缓存
  try {
    const mine = await getMyShop()
    if (mine?.id) {
      shopId.value = Number(mine.id)
      // 顺手把本地缓存补回来，否则缓存失效后只有这一个页面能用。
      session.saveShop({
        merchantId: mine.merchantId ?? stored?.merchantId,
        shopId: shopId.value,
        shopName: mine.shopName,
      })
    }
  } catch {
    /* ignore：使用缓存 shopId */
  }
  if (!shopId.value) {
    missingShop.value = true
    return
  }
  missingShop.value = false
  try {
    shop.value = await getShop(shopId.value)
    editName.value = shop.value.shopName || ''
    editNotice.value = shop.value.notice || ''
    openingTime.value = normalizeTime(shop.value.openingTime)
    closingTime.value = normalizeTime(shop.value.closingTime)
    // 回填已保存的封面，否则进页面永远显示占位图
    cover.value = shop.value.coverImageUrl || ''
    categories.value = await listCategories(shopId.value)
  } catch (error) {
    message.value = error?.message || '店铺加载失败，请稍后重试'
  }
}

async function saveShop() {
  message.value = ''
  savingShop.value = true
  try {
    const data = await updateShop(shopId.value, {
      shopName: editName.value.trim(),
      notice: editNotice.value.trim(),
    })
    shop.value.shopName = data.shopName
    message.value = '已保存'
  } catch (error) {
    message.value = error?.message || '保存失败，请稍后重试'
  } finally {
    savingShop.value = false
  }
}

async function onStatusChange() {
  message.value = ''
  try {
    const data = await changeStatus(shopId.value, shop.value.status)
    shop.value.status = data.status
    message.value = '营业状态已更新'
  } catch (error) {
    message.value = error?.message || '更新失败，请稍后重试'
  }
}

async function saveBusinessHours() {
  message.value = ''
  if (!openingTime.value || !closingTime.value) { message.value = '请选择开始和结束营业时间'; return }
  if (closingTime.value <= openingTime.value) { message.value = '结束营业时间必须晚于开始营业时间'; return }
  savingBusinessHours.value = true
  try {
    const data = await updateBusinessHours(shopId.value, openingTime.value, closingTime.value)
    openingTime.value = normalizeTime(data.openingTime)
    closingTime.value = normalizeTime(data.closingTime)
    message.value = '营业时间已保存'
  } catch (error) { message.value = error?.message || '营业时间保存失败，请稍后重试' }
  finally { savingBusinessHours.value = false }
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
    const created = await createCategory(shopId.value, { name, sort: Number.isNaN(sort) ? 0 : sort })
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

    <p v-if="message" data-testid="console-message" role="status" class="console-message">
      {{ message }}
    </p>

    <template v-if="shop">
      <section class="panel">
        <h3>店铺</h3>
        <ImageUploader
          v-model="cover"
          target-type="SHOP_COVER"
          :target-id="shopId"
          shape="wide"
          title="店铺封面"
          tip="顾客进店时店铺顶部的大图，建议横向"
          placeholder="店铺封面"
          upload-label="上传封面"
          replace-label="更换封面"
          input-testid="console-cover-input"
          @message="message = $event"
        />
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

        <div class="business-hours">
          <label class="ctrl-label" for="opening-time">开始营业时间</label>
          <input id="opening-time" v-model="openingTime" data-testid="opening-time-input" type="time" />
          <label class="ctrl-label" for="closing-time">结束营业时间</label>
          <input id="closing-time" v-model="closingTime" data-testid="closing-time-input" type="time" />
          <button type="button" class="primary-btn business-hours-save" data-testid="business-hours-save"
            :disabled="savingBusinessHours" @click="saveBusinessHours">
            {{ savingBusinessHours ? '保存中…' : '保存营业时间' }}
          </button>
        </div>

        <button
          class="primary-btn"
          data-testid="console-save-shop"
          :disabled="savingShop"
          @click="saveShop"
        >保存店铺资料</button>
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
            <template v-if="editingCatId === category.id">
              <el-input
                v-model="catNameEdit[category.id]"
                class="cat-name-inline"
                placeholder="分类名称"
              />
              <el-input
                v-model="catSortEdit[category.id]"
                class="cat-sort-inline"
                placeholder="排序"
              />
              <button class="link-btn" @click="saveCategory(category)">保存</button>
              <button class="link-btn" @click="editingCatId = null">取消</button>
            </template>
            <template v-else>
              <span>{{ category.name }}（排序 {{ category.sort }}）</span>
              <div class="cat-ops">
                <button class="link-btn" @click="startEditCategory(category)">编辑</button>
                <button
                  class="link-danger"
                  :data-testid="`category-delete-${category.id}`"
                  @click="removeCategory(category)"
                >删除</button>
              </div>
            </template>
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
.link-btn {
  border: none;
  background: transparent;
  color: var(--el-color-primary);
  cursor: pointer;
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
.business-hours { display:grid; grid-template-columns:auto minmax(8rem,1fr); align-items:center; gap:.75rem; margin:.35rem 0; }
.business-hours input[type='time'] { min-width:0; padding:.55rem .7rem; border:1px solid #dcdfe6; border-radius:8px; background:#fff; }
.business-hours-save { grid-column:1 / -1; }
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
  gap: 0.75rem;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 0.6rem 0.9rem;
  flex-wrap: wrap;
}
.cat-ops {
  display: flex;
  gap: 0.5rem;
}
.cat-name-inline {
  width: 10rem;
}
.cat-sort-inline {
  width: 5rem;
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
