<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrder, requestRefund } from '@/api/order'
import { isPositiveMoney } from '@/utils/money'

const route = useRoute()
const router = useRouter()
const orderId = Number(route.params.id)

const detail = ref(null)
const loadError = ref('')
const refundAmount = ref('')
const refundReason = ref('')
const refundEvidence = ref('')
const refundMessage = ref('')
const refundSubmitting = ref(false)

// 提交前的退款校验：金额格式、原因非空、证据不超过 3 个。
const EVIDENCE_MAX = 3
const refundEvidenceUrls = computed(() =>
  refundEvidence.value.split('\n').map((url) => url.trim()).filter(Boolean),
)
const refundBlockedReason = computed(() => {
  if (!isPositiveMoney(refundAmount.value)) return '金额需为 0.01～99999999.99 且最多两位小数'
  if (!refundReason.value.trim()) return '请填写退款原因'
  if (refundEvidenceUrls.value.length > EVIDENCE_MAX) return `退款证据最多 ${EVIDENCE_MAX} 个图片 URL`
  return ''
})
// 提交期间禁用，防止重复提交。
const canSubmitRefund = computed(() => !refundBlockedReason.value && !refundSubmitting.value)
const showRefundHint = computed(
  () => !!refundBlockedReason.value
    && (!!refundAmount.value.trim() || !!refundReason.value.trim() || !!refundEvidence.value.trim()),
)

// 支付只对未支付订单开放。支付成功后 status 仍是 CREATED，只改 paymentStatus，
// 所以要同时看 paymentStatus，否则已支付但尚未接单的订单会被再次引导去付款。
const canPay = computed(
  () => !!detail.value
    && detail.value.paymentStatus !== 'PAID'
    && detail.value.status === 'CREATED',
)

function fmt(value) {
  return (Number(value) || 0).toFixed(2)
}

function fmtTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
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

onMounted(async () => {
  try {
    detail.value = await getOrder(orderId)
  } catch {
    loadError.value = '未连接后端，无法加载订单详情（仅静态预览）'
  }
})

async function submitRefund() {
  if (refundSubmitting.value) return
  if (refundBlockedReason.value) {
    refundMessage.value = refundBlockedReason.value
    return
  }
  refundSubmitting.value = true
  refundMessage.value = ''
  try {
    await requestRefund(orderId, {
      // 上面已校验过格式，这里的 Number 不会产生指数形式或多于两位的小数。
      amount: Number(refundAmount.value.trim()),
      reason: refundReason.value.trim(),
      evidenceUrls: refundEvidenceUrls.value,
    })
    refundMessage.value = '退款申请已提交'
    refundAmount.value = ''
    refundReason.value = ''
    refundEvidence.value = ''
  } catch (e) {
    refundMessage.value = e?.message || '退款申请失败'
  } finally {
    refundSubmitting.value = false
  }
}
</script>

<template>
  <section class="order-detail">
    <button class="back-btn" @click="router.push('/orders')">← 返回订单列表</button>

    <template v-if="detail">
      <header class="detail-head">
        <h2 data-testid="order-no">{{ detail.orderNo }}</h2>
        <span data-testid="order-status" class="order-status">{{ detail.status }}</span>
      </header>
      <RouterLink
        v-if="canPay"
        :to="`/orders/${detail.id}/pay`"
        class="pay-link"
      >去支付 →</RouterLink>
      <p
        v-else-if="detail.paymentStatus === 'PAID' && detail.status === 'CREATED'"
        data-testid="order-paid-tip"
        class="pay-done-tip"
      >已支付，等待商家接单</p>
      <p class="order-time">{{ fmtTime(detail.createdAt) }}</p>

      <div class="shop-banner">
        <img v-if="detail.shopImage" :src="detail.shopImage" class="shop-thumb" alt="商家图" />
        <div v-else class="shop-thumb">🏪</div>
      </div>

      <div v-if="detail.shopPhone || detail.userPhoneMasked || detail.userAddress" class="contact-panel">
        <p v-if="detail.shopPhone">商家电话：{{ detail.shopPhone }}</p>
        <p v-if="detail.userPhoneMasked">联系电话：{{ detail.userPhoneMasked }}</p>
        <p v-if="detail.userAddress">收货地址：{{ detail.userAddress }}</p>
      </div>

      <ul class="item-list">
        <li
          v-for="item in detail.items"
          :key="item.productId"
          :data-testid="`order-item-${item.productId}`"
          class="item-row"
        >
          <img v-if="item.imageUrl" :src="item.imageUrl" class="dish-thumb" alt="菜品图" />
          <div v-else class="dish-thumb" :style="{ background: foodBg(item.productName) }">
            {{ foodEmoji(item.productName) }}
          </div>
          <span class="item-name">{{ item.productName }}</span>
          <span class="item-meta">
            ¥{{ fmt(item.unitPrice) }} × {{ item.quantity }}
          </span>
          <strong class="item-subtotal">¥{{ fmt(item.subtotal) }}</strong>
        </li>
      </ul>

      <footer class="detail-footer">
        合计
        <strong data-testid="order-total" class="order-total">¥{{ fmt(detail.totalAmount) }}</strong>
      </footer>
      <section v-if="detail.paymentStatus === 'PAID'" class="refund-box"><h3>申请退款</h3>
        <el-input
          v-model="refundAmount"
          type="text"
          inputmode="decimal"
          data-testid="refund-amount"
          placeholder="退款金额，最多两位小数"
        />
        <el-input
          v-model="refundReason"
          maxlength="255"
          data-testid="refund-reason"
          placeholder="退款原因"
        />
        <el-input
          v-model="refundEvidence"
          type="textarea"
          :rows="2"
          maxlength="765"
          data-testid="refund-evidence"
          placeholder="证据图片 URL，每行一个，最多 3 个（可留空）"
        />
        <button
          :disabled="!canSubmitRefund"
          data-testid="refund-submit"
          @click="submitRefund"
        >{{ refundSubmitting ? '提交中…' : '提交退款申请' }}</button>
        <p v-if="showRefundHint" data-testid="refund-hint" class="refund-hint">{{ refundBlockedReason }}</p>
        <p v-if="refundMessage">{{ refundMessage }}</p></section>
    </template>
    <p v-else class="loading-tip">{{ loadError || '订单加载中…' }}</p>
  </section>
</template>

<style scoped>
.order-detail {
  max-width: 42rem;
  margin: 0 auto;
  background: #fff;
  border-radius: var(--card-radius);
  padding: 1.5rem;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.back-btn {
  align-self: flex-start;
  border: none;
  background: transparent;
  color: #666;
  cursor: pointer;
  padding: 0;
}
.back-btn:hover {
  color: #ff6a00;
}
.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}
.detail-head h2 {
  margin: 0;
  font-size: 1.2rem;
}
.order-status {
  font-size: 0.85rem;
  color: #ff6a00;
  background: #fff4ea;
  border-radius: 999px;
  padding: 0.2rem 0.7rem;
}
.order-time {
  margin: 0;
  color: #aaa;
  font-size: 0.85rem;
}
.shop-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.shop-thumb {
  /* 3rem × 1.5 */
  width: 4.5rem;
  height: 4.5rem;
  border-radius: 10px;
  object-fit: cover;
  background: #fff4ec;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.4rem;
}
.dish-thumb {
  flex-shrink: 0;
  /* 2.6rem × 1.5 */
  width: 3.9rem;
  height: 3.9rem;
  border-radius: 8px;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.1rem;
}
.contact-panel {
  background: #fff7f0;
  border: 1px solid #ffe3cf;
  border-radius: 10px;
  padding: 0.7rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}
.contact-panel p {
  margin: 0;
  font-size: 0.9rem;
  color: #4a3a2a;
}
.item-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
.item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 1rem;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
}
.item-name {
  font-weight: 600;
}
.item-meta {
  color: #999;
  font-size: 0.9rem;
}
.item-subtotal {
  color: #ff6a00;
}
.detail-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.6rem;
  border-top: 1px dashed #eee;
  padding-top: 0.9rem;
  color: #666;
}
.order-total {
  color: #ff6a00;
  font-size: 1.2rem;
}
.loading-tip {
  color: #aaa;
  text-align: center;
}
.refund-box{border-top:1px dashed #eee;padding-top:1rem;display:grid;gap:.6rem}.refund-box h3{margin:0}.refund-box button{border:0;border-radius:999px;padding:.5rem;background:var(--brand-gradient)}
.refund-box button:disabled{opacity:.5;cursor:not-allowed}.refund-box p{margin:0;font-size:.85rem}
.refund-hint{color:#e34d1c}
.pay-done-tip{margin:0;font-size:.85rem;color:#52a05a}
.detail-head {
  flex-wrap: wrap;
}
.detail-head h2 {
  word-break: break-all;
}
.item-row {
  flex-wrap: wrap;
}
.contact-panel p {
  overflow-wrap: anywhere;
}
@media (max-width: 520px) {
  .order-detail {
    padding: 1rem;
    gap: 0.6rem;
  }
  .detail-head h2 {
    font-size: 1rem;
  }
  .dish-thumb {
    /* 2.2rem × 1.5 */
    width: 3.3rem;
    height: 3.3rem;
    font-size: 1.65rem;
  }
  .item-name {
    font-size: 0.92rem;
  }
}
</style>
