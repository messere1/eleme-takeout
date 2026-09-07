<script setup>
import { onMounted, ref } from 'vue'
import { acceptOrder, completeOrder, listMerchantOrders } from '@/api/order'
import { session } from '@/utils/session'

const STATUS_TEXT = {
  PENDING: '待接单',
  ACCEPTED: '已接单',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

const stored = session.loadShop()
const shopId = stored?.shopId
const missingShop = !shopId

const orders = ref([])
const message = ref('')

function fmt(value) {
  return (Number(value) || 0).toFixed(2)
}

function statusText(status) {
  return STATUS_TEXT[status] || status
}

async function load() {
  if (missingShop) return
  try {
    orders.value = (await listMerchantOrders()) || []
  } catch (error) {
    message.value = error?.message || '加载订单失败'
  }
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

async function complete(order) {
  message.value = ''
  try {
    const res = await completeOrder(order.id)
    order.status = res?.status || 'COMPLETED'
  } catch (error) {
    message.value = error?.message || '操作失败，请稍后重试'
  }
}

onMounted(load)
</script>

<template>
  <section class="console">
    <h2>订单管理</h2>

    <p v-if="missingShop" class="console-missing">
      还没有店铺？<RouterLink to="/register">去注册开店</RouterLink>，或
      <RouterLink to="/login">用账号登录</RouterLink>。
    </p>

    <template v-else>
      <p v-if="message" class="console-message">{{ message }}</p>

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
            <button
              v-if="order.status === 'PENDING'"
              class="primary-btn small"
              :data-testid="`order-mgmt-accept-${order.id}`"
              @click="accept(order)"
            >接单</button>
            <button
              v-if="order.status === 'ACCEPTED'"
              class="primary-btn small"
              :data-testid="`order-mgmt-complete-${order.id}`"
              @click="complete(order)"
            >完成</button>
          </div>
        </li>
      </ul>
      <div v-else data-testid="order-mgmt-empty" class="orders-empty">暂无订单</div>
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
  gap: 0.5rem;
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
</style>
