<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearCart, getCart, removeItem, updateItem, saveDeliveryInfo } from '@/api/cart'
import { createOrder } from '@/api/order'
import { getProfile } from '@/api/user'

const router = useRouter()

const items = ref([])
const busy = ref(false)
const message = ref('')
const loadError = ref('')
const checkoutAddress = ref('')
const recipient = ref('')
const contact = ref('')
const drawerOpen = ref(false)
const saveToProfile = ref(false)

async function seedDefaultAddress() {
  try {
    const me = await getProfile()
    if (me?.address) checkoutAddress.value = me.address
    if (me?.username) recipient.value = me.username
    if (me?.phone) contact.value = me.phone
  } catch {
    // 未填资料则让用户手动输入
  }
}

const total = computed(() =>
  items.value.reduce((sum, item) => sum + item.price * item.quantity, 0),
)
const empty = computed(() => items.value.length === 0)
const hasUnavailable = computed(() => items.value.some((item) => !item.available))
const canCheckout = computed(() => !empty.value && !hasUnavailable.value && !busy.value
  && recipient.value.trim() && /^[+0-9 -]{7,20}$/.test(contact.value.trim())
  && checkoutAddress.value.trim().length >= 5)

function fmt(value) {
  return (Number(value) || 0).toFixed(2)
}

async function load() {
  try {
    const cart = await getCart()
    items.value = cart?.items ?? []
  } catch (error) {
    if (error?.code === 'AUTH_INVALID' || error?.code === 'AUTH_EXPIRED') {
      router.push('/login')
      return
    }
    loadError.value = '未连接后端，无法加载购物车（仅静态预览）'
  }
}

async function plus(item) {
  message.value = ''
  try {
    await updateItem(item.id, item.quantity + 1)
    item.quantity += 1
  } catch (error) {
    if (error?.code === 'AUTH_INVALID' || error?.code === 'AUTH_EXPIRED') {
      router.push('/login')
      return
    }
    message.value = error?.message || '修改数量失败，请稍后重试'
  }
}

async function minus(item) {
  message.value = ''
  if (item.quantity <= 1) {
    try {
      await removeItem(item.id)
      items.value = items.value.filter((entry) => entry.id !== item.id)
    } catch (error) {
      message.value = error?.message || '删除失败，请稍后重试'
    }
    return
  }
  try {
    await updateItem(item.id, item.quantity - 1)
    item.quantity -= 1
  } catch (error) {
    message.value = error?.message || '修改数量失败，请稍后重试'
  }
}

async function remove(item) {
  await removeItem(item.id)
  items.value = items.value.filter((entry) => entry.id !== item.id)
}

async function clearAll() {
  await clearCart()
  items.value = []
}

async function checkout() {
  message.value = ''
  busy.value = true
  try {
    const order = await createOrder({
      shopId: items.value[0]?.shopId,
      recipientName: recipient.value.trim(),
      recipientPhone: contact.value.trim(),
      deliveryAddress: checkoutAddress.value.trim(),
      saveToProfile: saveToProfile.value,
    })
    await saveDeliveryInfo({ shopId: items.value[0]?.shopId, recipientName: recipient.value.trim(),
      recipientPhone: contact.value.trim(), deliveryAddress: checkoutAddress.value.trim(), saveToProfile: saveToProfile.value })
    router.push(`/orders/${order.id}`)
  } catch (error) {
    if (error?.code === 'AUTH_INVALID' || error?.code === 'AUTH_EXPIRED') {
      router.push('/login')
      return
    }
    message.value = error?.message || '下单失败，请稍后重试'
  } finally {
    busy.value = false
  }
}

onMounted(() => {
  seedDefaultAddress()
  load()
})
</script>

<template>
  <section class="cart-page">
    <h2>我的购物车</h2>
    <p v-if="loadError" class="cart-message">{{ loadError }}</p>

    <template v-if="!empty">
      <ul class="cart-list">
        <li
          v-for="item in items"
          :key="item.id"
          :data-testid="`cart-item-${item.id}`"
          class="cart-item"
          :class="{ unavailable: !item.available }"
        >
          <div class="cart-info">
            <div class="cart-name-line">
              <strong>{{ item.productName }}</strong>
              <span v-if="!item.available" class="off-tag">已下架</span>
            </div>
            <span class="cart-price">¥{{ fmt(item.price) }}</span>
          </div>

          <div class="cart-ops">
            <button
              class="qty-btn"
              :data-testid="`cart-minus-${item.id}`"
              :disabled="busy"
              @click="minus(item)"
            >−</button>
            <span :data-testid="`cart-qty-${item.id}`" class="qty-num">{{ item.quantity }}</span>
            <button
              class="qty-btn"
              :data-testid="`cart-plus-${item.id}`"
              :disabled="busy"
              @click="plus(item)"
            >+</button>
            <button
              class="remove-btn"
              :data-testid="`cart-remove-${item.id}`"
              :disabled="busy"
              @click="remove(item)"
            >删除</button>
          </div>
        </li>
      </ul>

      <div class="address-row">
        <label class="ctrl-label" for="cart-address">收货地址</label>
        <el-input
          id="cart-address"
          v-model="checkoutAddress"
          data-testid="cart-address"
          placeholder="默认取「我的-收货地址」，可直接修改"
          maxlength="255"
        />
      </div>

      <footer class="cart-footer">
        <span>
          合计
          <strong data-testid="cart-total" class="cart-total">¥{{ fmt(total) }}</strong>
        </span>
        <div class="footer-actions">
          <button
            class="clear-btn"
            data-testid="cart-clear"
            :disabled="empty || busy"
            @click="clearAll"
          >清空</button>
          <button
            class="checkout-btn"
            data-testid="cart-checkout"
            :disabled="!canCheckout"
            @click="checkout"
          >去结算</button>
        </div>
      </footer>

      <div class="cart-drawer-bar" @click="drawerOpen = !drawerOpen">
        <span>{{ drawerOpen ? '▼ 收起明细' : '▲ 订单明细' }}（{{ items.length }} 件）</span>
        <strong>合计 ¥{{ fmt(total) }}</strong>
      </div>

      <div v-if="drawerOpen" class="cart-sheet">
        <div class="sheet-head">
          <h3>订单信息</h3>
          <button class="sheet-close" @click="drawerOpen = false">收起 ▲</button>
        </div>
        <ul class="sheet-items">
          <li v-for="item in items" :key="item.id">
            <span>{{ item.productName }} × {{ item.quantity }}</span>
            <strong>¥{{ fmt(item.subtotal) }}</strong>
          </li>
        </ul>
        <div class="field"><label>收货人</label>
          <el-input v-model="recipient" data-testid="cart-recipient" placeholder="收货人" maxlength="30" /></div>
        <div class="field"><label>联系电话</label>
          <el-input v-model="contact" data-testid="cart-contact" placeholder="联系电话" maxlength="11" /></div>
        <div class="field"><label>收货地址</label>
          <el-input v-model="checkoutAddress" placeholder="收货地址" maxlength="255" /></div>
        <label class="save-profile"><input v-model="saveToProfile" type="checkbox" /> 保存为个人默认地址</label>
        <div class="sheet-total">合计 <strong>¥{{ fmt(total) }}</strong></div>
        <button class="checkout-btn sheet-checkout" :disabled="!canCheckout" @click="checkout">提交订单</button>
      </div>

      <p v-if="message" class="cart-message">{{ message }}</p>
    </template>

    <div v-else data-testid="cart-empty" class="cart-empty">
      <p>购物车是空的</p>
      <RouterLink to="/shops/1">去逛逛</RouterLink>
    </div>
  </section>
</template>

<style scoped>
.cart-page {
  max-width: 40rem;
  margin: 0 auto;
  background: #fff;
  border-radius: var(--card-radius);
  padding: 1.5rem;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.06);
}
.cart-page h2 {
  margin: 0 0 1rem;
}
.cart-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.cart-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.9rem 1rem;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
}
.cart-item.unavailable {
  opacity: 0.6;
}
.cart-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.cart-name-line {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.off-tag {
  font-size: 0.75rem;
  color: #999;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 0 0.3rem;
}
.cart-price {
  color: #ff6a00;
}
.cart-ops {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.qty-btn {
  width: 1.6rem;
  height: 1.6rem;
  border-radius: 50%;
  border: 1px solid #ddd;
  background: #fff;
  cursor: pointer;
  line-height: 1;
}
.qty-num {
  min-width: 1.2rem;
  text-align: center;
}
.remove-btn {
  border: none;
  background: transparent;
  color: #e34d1c;
  cursor: pointer;
  font-size: 0.85rem;
  padding: 0.25rem 0.4rem;
}
.remove-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.cart-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px dashed #eee;
}
.cart-total {
  color: #ff6a00;
  font-size: 1.2rem;
}
.footer-actions {
  display: flex;
  gap: 0.5rem;
}
.clear-btn {
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 999px;
  padding: 0.4rem 1rem;
  cursor: pointer;
  color: #666;
}
.checkout-btn {
  border: none;
  background: var(--brand-gradient);
  color: #2b1d00;
  font-weight: 700;
  border-radius: 999px;
  padding: 0.45rem 1.3rem;
  cursor: pointer;
}
.checkout-btn:disabled,
.clear-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.cart-message {
  margin: 0.75rem 0 0;
  color: #e34d1c;
}
.cart-empty {
  text-align: center;
  color: #999;
  padding: 2rem 0;
}
.cart-empty a {
  color: #ff6a00;
  font-weight: 600;
}
.address-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1rem;
  flex-wrap: wrap;
}
.ctrl-label {
  font-size: 0.9rem;
  color: #666;
  white-space: nowrap;
}
.address-row :deep(.el-input) {
  flex: 1;
  min-width: 16rem;
}
.cart-drawer-bar {
  position: sticky;
  bottom: 0.5rem;
  margin-top: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #2b1d00;
  color: #ffe08a;
  border-radius: 999px;
  padding: 0.7rem 1.2rem;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.25);
  cursor: pointer;
}
.cart-sheet {
  background: #fff;
  border: 1px solid #f0e6da;
  border-radius: 16px;
  padding: 1rem 1.2rem 1.2rem;
  box-shadow: 0 -6px 22px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
  margin-top: 0.5rem;
}
.sheet-head { display: flex; align-items: center; justify-content: space-between; }
.sheet-head h3 { margin: 0; }
.sheet-close { border: none; background: transparent; color: var(--el-color-primary); cursor: pointer; }
.sheet-items { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.35rem; }
.sheet-items li { display: flex; justify-content: space-between; font-size: 0.92rem; }
.sheet-items li strong { color: #ff2f00; }
.sheet-total { display: flex; justify-content: space-between; border-top: 1px dashed #eee; padding-top: 0.7rem; }
.sheet-total strong { color: #ff2f00; }
.sheet-checkout { width: 100%; }
.field { display: flex; flex-direction: column; gap: 0.35rem; }
.field label { font-size: 0.85rem; color: #666; }
@media (max-width: 520px) {
  .cart-page {
    padding: 1rem;
  }
  .cart-ops {
    flex-wrap: wrap;
    justify-content: flex-end;
  }
  .cart-footer {
    flex-direction: column;
    align-items: stretch;
    gap: 0.6rem;
  }
  .footer-actions {
    justify-content: flex-end;
  }
}
</style>
