<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { cancelOrder, confirmOrder, listOrders } from '@/api/order'

const router = useRouter()

const orders = ref([])
const currentPage = ref(1)
const totalPages = ref(1)
const selectedStatus = ref('')
const loadError = ref('')

function fmt(value) {
  return (Number(value) || 0).toFixed(2)
}

function fmtTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

const STATUS_TEXT = { CREATED: '待处理', CANCELLED: '已取消', ACCEPTED: '已接单', COMPLETED: '已完成', PENDING: '待处理' }

function statusText(status) {
  return STATUS_TEXT[status] || status
}

async function cancelRow(order) {
  loadError.value = ''
  try {
    const res = await cancelOrder(order.id)
    order.status = res?.status || 'CANCELLED'
  } catch (error) {
    loadError.value = error?.message || '取消失败，请稍后重试'
  }
}

async function confirmRow(order) {
  loadError.value = ''
  try {
    const res = await confirmOrder(order.id)
    order.status = res?.status || 'COMPLETED'
  } catch (error) {
    loadError.value = error?.message || '确认失败，请稍后重试'
  }
}

async function load(page) {
  try {
    const params = { page, size: 20 }
    if (selectedStatus.value) params.status = selectedStatus.value
    const data = await listOrders(params)
    orders.value = data?.items ?? []
    currentPage.value = data?.page ?? page
    totalPages.value = data?.totalPages ?? 1
  } catch (error) {
    if (error?.code === 'AUTH_INVALID' || error?.code === 'AUTH_EXPIRED') {
      router.push('/login')
      return
    }
    loadError.value = '无法加载订单：' + (error?.message || '请确认已登录且后端服务正常')
  }
}

function onStatusChange() {
  load(1)
}

function go(page) {
  if (page < 1 || page > totalPages.value) return
  load(page)
}

function goDetail(order) {
  router.push(`/orders/${order.id}`)
}

onMounted(() => load(1))
</script>

<template>
  <section class="orders-page">
    <h2>我的订单</h2>
    <p v-if="loadError" class="page-note">{{ loadError }}</p>

    <div class="orders-toolbar">
      <label for="orders-status">状态</label>
      <select
        id="orders-status"
        v-model="selectedStatus"
        data-testid="orders-status"
        class="status-select"
        @change="onStatusChange"
      >
        <option value="">全部</option>
        <option value="CREATED">待处理</option>
      </select>
    </div>

    <template v-if="orders.length">
      <ul class="order-list">
        <li
          v-for="order in orders"
          :key="order.id"
          :data-testid="`order-row-${order.id}`"
          class="order-row"
          @click="goDetail(order)"
        >
          <div class="order-main">
            <span class="order-no">{{ order.orderNo }}</span>
            <span class="order-status">{{ statusText(order.status) }}</span>
          </div>
          <div class="order-sub">
            <span class="order-time">{{ fmtTime(order.createdAt) }}</span>
            <strong class="order-amount">¥{{ fmt(order.totalAmount) }}</strong>
          </div>
          <div v-if="order.status === 'CREATED' || order.status === 'PENDING'" class="order-cancel">
            <button
              class="cancel-btn"
              :data-testid="`order-cancel-${order.id}`"
              @click.stop="cancelRow(order)"
            >取消订单</button>
          </div>
          <div v-else-if="order.status === 'ACCEPTED'" class="order-confirm">
            <button
              class="confirm-btn"
              :data-testid="`order-confirm-${order.id}`"
              @click.stop="confirmRow(order)"
            >确认完成</button>
          </div>
        </li>
      </ul>

      <footer class="order-pager">
        <button
          data-testid="orders-prev"
          class="page-btn"
          :disabled="currentPage <= 1"
          @click="go(currentPage - 1)"
        >上一页</button>
        <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
        <button
          data-testid="orders-next"
          class="page-btn"
          :disabled="currentPage >= totalPages"
          @click="go(currentPage + 1)"
        >下一页</button>
      </footer>
    </template>

    <div v-else data-testid="orders-empty" class="orders-empty">暂无订单</div>
  </section>
</template>

<style scoped>
.orders-page {
  max-width: 42rem;
  margin: 0 auto;
  background: #fff;
  border-radius: var(--card-radius);
  padding: 1.5rem;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.06);
}
.orders-page h2 {
  margin: 0 0 1rem;
}
.order-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.order-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.9rem 1rem;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  cursor: pointer;
  transition: box-shadow 0.12s ease;
}
.order-row:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.order-main {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.order-no {
  font-weight: 600;
}
.order-status {
  font-size: 0.8rem;
  color: #ff6a00;
  background: #fff4ea;
  border-radius: 999px;
  padding: 0.1rem 0.5rem;
  align-self: flex-start;
}
.order-sub {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}
.order-time {
  color: #aaa;
  font-size: 0.8rem;
}
.order-amount {
  color: #ff6a00;
}
.order-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-top: 1rem;
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
.orders-empty {
  text-align: center;
  color: #999;
  padding: 2.5rem 0;
}
.cancel-btn {
  border: 1px solid #ff6a00;
  color: #ff6a00;
  background: #fff;
  border-radius: 999px;
  padding: 0.3rem 0.8rem;
  cursor: pointer;
  font-size: 0.85rem;
  white-space: nowrap;
}
.confirm-btn {
  border: none;
  background: #17a25c;
  color: #fff;
  border-radius: 999px;
  padding: 0.32rem 0.9rem;
  cursor: pointer;
  font-size: 0.85rem;
  white-space: nowrap;
}
.page-note {
  margin: 0 0 0.75rem;
  color: #e34d1c;
  font-size: 0.9rem;
}
.orders-toolbar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}
.orders-toolbar label {
  color: #666;
  font-size: 0.9rem;
}
.status-select {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 0.3rem 0.6rem;
  background: #fff;
  color: #444;
}
</style>
