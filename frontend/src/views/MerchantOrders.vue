<script setup>
import { onMounted, ref } from 'vue'
import { acceptOrder, listMerchantOrders, listMerchantRefunds, decideMerchantRefund } from '@/api/order'

// §5.1 状态机的全部取值，前端不自造状态（原来的 PENDING 后端从不产生）。
const STATUS_TEXT = {
  CREATED: '待接单',
  ACCEPTED: '已接单',
  DELIVERING: '配送中',
  DELIVERED: '已送达',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}
const STATUS_OPTIONS = Object.keys(STATUS_TEXT)

const orders = ref([])
const message = ref('')
const refunds = ref([])
const selectedStatus = ref('')
const startTime = ref('')
const endTime = ref('')
const currentPage = ref(1)
const totalPages = ref(1)

function fmt(value) {
  return (Number(value) || 0).toFixed(2)
}

function statusText(status) {
  return STATUS_TEXT[status] || status
}

// FR-020 / EX-016：只有已支付订单才能接单。
function canAccept(order) {
  return order.status === 'CREATED' && order.paymentStatus === 'PAID'
}

// FR-016：顾客、商家、管理员都要能按分页、状态、时间查询。
function query(page) {
  const params = { page, size: 20 }
  if (selectedStatus.value) params.status = selectedStatus.value
  if (startTime.value) params.startTime = startTime.value
  if (endTime.value) params.endTime = endTime.value
  return params
}

async function load(page = 1) {
  try {
    const [orderData,refundData]=await Promise.all([listMerchantOrders(query(page)),listMerchantRefunds()])
    orders.value=orderData?.items ?? (Array.isArray(orderData) ? orderData : [])
    currentPage.value = orderData?.page ?? page
    totalPages.value = orderData?.totalPages ?? 1
    refunds.value=refundData||[]
  } catch (error) {
    message.value = error?.message || '加载订单失败'
  }
}

function applyFilters() {
  load(1)
}

function go(page) {
  if (page < 1 || page > totalPages.value) return
  load(page)
}

async function accept(order) {
  message.value = ''
  try {
    const res = await acceptOrder(order.id)
    order.status = res?.status || 'ACCEPTED'
  } catch (error) {
    message.value = error?.message || '接单失败，请稍后重试'
  }
}

// 订单完成不由商家触发：§5.1 的完成路径是「DELIVERED → 顾客确认收货」（FR-022），
// §9 接口表里也没有商家完成的接口，所以这里只说明流程，不发请求。
function complete(order) {
  message.value = `订单 ${order.orderNo} 需骑手送达后由顾客确认收货才能完成`
}

onMounted(() => load(1))
async function decideRefund(r,status){try{const x=await decideMerchantRefund(r.id,status);r.status=x.status}catch(e){message.value=e?.message||'退款处理失败'}}
</script>

<template>
  <section class="console">
    <h2>订单管理</h2>

    <p v-if="message" class="console-message">{{ message }}</p>

    <div class="order-filters">
      <label for="order-mgmt-status">状态</label>
      <select
        id="order-mgmt-status"
        v-model="selectedStatus"
        data-testid="order-mgmt-status"
        class="status-select"
        @change="applyFilters"
      >
        <option value="">全部</option>
        <option v-for="s in STATUS_OPTIONS" :key="s" :value="s">{{ statusText(s) }}</option>
      </select>
      <label for="order-mgmt-start">起</label>
      <input
        id="order-mgmt-start"
        v-model="startTime"
        data-testid="order-mgmt-start"
        type="datetime-local"
        @change="applyFilters"
      />
      <label for="order-mgmt-end">止</label>
      <input
        id="order-mgmt-end"
        v-model="endTime"
        data-testid="order-mgmt-end"
        type="datetime-local"
        @change="applyFilters"
      />
    </div>

    <ul v-if="orders.length" class="order-list">
        <li
          v-for="order in orders"
          :key="order.id"
          :data-testid="`order-mgmt-item-${order.id}`"
          class="order-row"
        >
          <div class="order-main">
            <span class="order-no">{{ order.orderNo }}</span>
            <span class="order-status">{{ statusText(order.status) }}</span>
          </div>
          <div class="order-amount">¥{{ fmt(order.totalAmount) }}</div>
          <div class="order-actions">
            <!-- FR-020「已支付订单才能进入履约」：CREATED 订单支付状态仍是 CREATED，
                 只看 status 会把未支付订单的接单按钮摆出来，点了必然 409（EX-016）。 -->
            <button
              v-if="canAccept(order)"
              class="primary-btn small"
              :data-testid="`order-mgmt-accept-${order.id}`"
              @click="accept(order)"
            >接单</button>
            <span
              v-else-if="order.status === 'CREATED'"
              :data-testid="`order-mgmt-unpaid-${order.id}`"
              class="order-hint"
            >等待顾客支付</span>
            <button
              v-if="order.status === 'ACCEPTED'"
              class="primary-btn small"
              :data-testid="`order-mgmt-complete-${order.id}`"
              @click="complete(order)"
            >完成</button>
            <span v-if="order.status === 'ACCEPTED'" class="order-hint">送达后由顾客确认收货</span>
          </div>
        </li>
      </ul>
    <div v-else data-testid="order-mgmt-empty" class="orders-empty">暂无订单</div>

    <footer v-if="totalPages > 1" class="order-pager">
      <button
        data-testid="order-mgmt-prev"
        class="page-btn"
        :disabled="currentPage <= 1"
        @click="go(currentPage - 1)"
      >上一页</button>
      <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
      <button
        data-testid="order-mgmt-next"
        class="page-btn"
        :disabled="currentPage >= totalPages"
        @click="go(currentPage + 1)"
      >下一页</button>
    </footer>

    <h2>退款申请</h2><div v-if="!refunds.length" class="orders-empty">暂无退款申请</div>
    <div v-for="r in refunds" :key="r.id" class="order-row"><span>订单 {{r.orderId}} · ¥{{fmt(r.amount)}} · {{r.reason}}</span><span>{{r.status}}</span><div v-if="r.status==='PENDING'"><button @click="decideRefund(r,'APPROVED')">批准</button><button @click="decideRefund(r,'REJECTED')">拒绝</button></div></div>
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
.order-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.order-row {
  background: #fff;
  border-radius: var(--card-radius);
  padding: 0.8rem 1rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}
.order-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.order-no {
  font-weight: 600;
}
.order-status {
  align-self: flex-start;
  font-size: 0.78rem;
  background: #fff4ec;
  color: #ff6a00;
  border-radius: 999px;
  padding: 0.1rem 0.5rem;
}
.order-amount {
  color: #ff2f00;
  font-weight: 700;
}
.order-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.order-hint {
  color: #aaa;
  font-size: 0.78rem;
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
  padding: 0.35rem 1rem;
  font-size: 0.9rem;
}
.orders-empty {
  color: #aaa;
  text-align: center;
  padding: 2rem 0;
}
.order-filters {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  font-size: 0.85rem;
  color: #666;
}
.order-filters select,
.order-filters input {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 0.35rem 0.5rem;
  font: inherit;
}
.order-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.8rem;
}
.page-btn {
  border: 1px solid #eee;
  background: #fff;
  border-radius: 999px;
  padding: 0.35rem 1rem;
  cursor: pointer;
}
.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.page-info {
  color: #999;
  font-size: 0.85rem;
}
</style>
