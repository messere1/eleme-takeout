<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrder, payOrder } from '@/api/order'

const route = useRoute()
const router = useRouter()
const orderId = Number(route.params.id)

const order = ref(null)
const message = ref('')
const paying = ref(false)
const now = ref(Date.now())
let timer
// §9.1：倒计时以服务器给的 paymentDeadline 为准。截止时间为空的历史订单不能
// 用 new Date(0) 顶替——那样会永远显示 00:00、按钮永久禁用，还会被本地误判成"已取消"。
const hasDeadline = computed(()=>!!order.value?.paymentDeadline)
const remaining = computed(()=>{
  if(!hasDeadline.value) return null
  return Math.max(0,Math.floor((new Date(order.value.paymentDeadline).getTime()-now.value)/1000))
})
const countdown = computed(()=>{
  if(remaining.value===null) return ''
  return `${String(Math.floor(remaining.value/60)).padStart(2,'0')}:${String(remaining.value%60).padStart(2,'0')}`
})
const expired = computed(()=>hasDeadline.value && remaining.value===0)

function fmt(value) {
  return (Number(value) || 0).toFixed(2)
}

onMounted(async () => {
  try {
    order.value = await getOrder(orderId)
    // 只在拿到服务器截止时间时才本地判定超时；否则交给后端判定（FR-018/EX-013）。
    timer=setInterval(()=>{now.value=Date.now();if(expired.value){clearInterval(timer);order.value.status='CANCELLED'}},1000)
  } catch (error) {
    message.value = '无法加载订单：' + (error?.message || '请稍后重试')
  }
})
onBeforeUnmount(()=>clearInterval(timer))

async function pay() {
  if (paying.value) return
  message.value = ''
  paying.value = true
  try {
    await payOrder(orderId)
    router.push(`/orders/${orderId}`)
  } catch (error) {
    message.value = '支付接口暂不可用（后端待提供）：' + (error?.message || '')
  } finally {
    paying.value = false
  }
}
</script>

<template>
  <section class="pay-page">
    <h2>收银台</h2>
    <p v-if="message" class="pay-message">{{ message }}</p>

    <template v-if="order">
      <div class="pay-box">
        <p class="order-no">订单号：{{ order.orderNo }}</p>
        <div class="amount">
          <span>应付金额</span>
          <strong>¥{{ fmt(order.totalAmount) }}</strong>
        </div>
        <p class="tip">
          <template v-if="hasDeadline">请在 {{ countdown }} 内完成支付，超时订单将自动取消并回补库存</template>
          <template v-else>请在 15 分钟内完成支付，超时订单将自动取消并回补库存</template>
        </p>
      </div>
      <button class="pay-btn" :disabled="paying || expired || order.status === 'CANCELLED'" data-testid="pay-submit" @click="pay">
        {{ paying ? '支付中…' : '立即支付' }}
      </button>
    </template>
  </section>
</template>

<style scoped>
.pay-page {
  max-width: 30rem;
  margin: 1.5rem auto;
  background: #fff;
  border-radius: var(--card-radius);
  padding: 1.5rem;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.06);
}
.pay-page h2 { margin: 0 0 1rem; }
.pay-message { color: #e34d1c; }
.pay-box { display: flex; flex-direction: column; gap: 0.75rem; }
.order-no { color: #999; }
.amount { display: flex; justify-content: space-between; align-items: baseline; }
.amount strong { color: #ff2f00; font-size: 2rem; }
.tip { color: #aaa; font-size: 0.85rem; }
.pay-btn {
  width: 100%;
  border: none;
  background: var(--brand-gradient);
  color: #fff;
  font-weight: 700;
  font-size: 1.05rem;
  border-radius: 999px;
  padding: 0.8rem 0;
  cursor: pointer;
  margin-top: 1rem;
}
</style>
