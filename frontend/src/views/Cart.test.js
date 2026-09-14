// @vitest-environment jsdom
// 购物车页红灯基线。mock 掉 api 模块；页面须提供 data-testid：
// cart-item-<id> / cart-qty-<id> / cart-plus-<id> / cart-minus-<id> / cart-clear /
// cart-checkout / cart-total / cart-empty。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/cart', () => ({
  getCart: vi.fn(),
  updateItem: vi.fn(),
  removeItem: vi.fn(),
  clearCart: vi.fn(),
  saveDeliveryInfo: vi.fn(),
}))
vi.mock('@/api/order', () => ({ createOrder: vi.fn() }))
vi.mock('@/api/user', () => ({ getProfile: vi.fn() }))

import { clearCart, getCart, removeItem, updateItem } from '@/api/cart'
import { createOrder } from '@/api/order'
import { getProfile } from '@/api/user'
import { mountView } from '@/test/mountView'
import Cart from './Cart.vue'

const FULL_CART = {
  items: [
    { id: 50, shopId: 20, productId: 40, productName: '煎饼果子', price: 8.5, quantity: 2, subtotal: 17.0, available: true, unavailableReason: null },
    { id: 51, productId: 44, productName: '柠檬茶', price: 6.0, quantity: 1, subtotal: 6.0, available: false, unavailableReason: 'OFF_SALE' },
  ],
  totalAmount: 23.0,
}
const AVAILABLE_CART = {
  items: [
    { id: 50, shopId: 20, productId: 40, productName: '煎饼果子', price: 8.5, quantity: 2, subtotal: 17.0, available: true, unavailableReason: null },
  ],
  totalAmount: 17.0,
}

async function mountCart(cart = FULL_CART) {
  getCart.mockResolvedValue(cart)
  const ctx = await mountView(Cart, { path: '/cart' })
  await flushPromises()
  return ctx
}

describe('购物车页', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getProfile.mockResolvedValue({ username: '默认收货人', phone: '13800138000', address: '天津大学北洋园校区' })
  })

  it('展示商品条目、不可用提示与合计金额', async () => {
    const { wrapper } = await mountCart()
    expect(wrapper.text()).toContain('煎饼果子')
    expect(wrapper.text()).toContain('柠檬茶')
    expect(wrapper.get('[data-testid="cart-item-51"]').text()).toContain('已下架')
    expect(wrapper.get('[data-testid="cart-total"]').text()).toContain('23.00')
  })

  it('点击加号调用 updateItem 并更新数量与合计', async () => {
    updateItem.mockResolvedValue({ id: 50, productId: 40, quantity: 3 })
    const { wrapper } = await mountCart()
    await wrapper.get('[data-testid="cart-plus-50"]').trigger('click')
    await flushPromises()
    expect(updateItem).toHaveBeenCalledWith(50, 3)
    expect(wrapper.get('[data-testid="cart-qty-50"]').text()).toContain('3')
    expect(wrapper.get('[data-testid="cart-total"]').text()).toContain('31.50')
  })

  it('数量为 1 时点减号触发删除该行', async () => {
    removeItem.mockResolvedValue(null)
    const { wrapper } = await mountCart()
    await wrapper.get('[data-testid="cart-minus-51"]').trigger('click')
    await flushPromises()
    expect(removeItem).toHaveBeenCalledWith(51)
    expect(wrapper.find('[data-testid="cart-item-51"]').exists()).toBe(false)
  })

  it('清空购物车后展示空状态', async () => {
    clearCart.mockResolvedValue(null)
    const { wrapper } = await mountCart()
    await wrapper.get('[data-testid="cart-clear"]').trigger('click')
    await flushPromises()
    expect(clearCart).toHaveBeenCalled()
    expect(wrapper.get('[data-testid="cart-empty"]').text()).toContain('购物车是空的')
  })

  it('存在不可用商品时「去结算」按钮禁用', async () => {
    const { wrapper } = await mountCart()
    expect(wrapper.get('[data-testid="cart-checkout"]').element.disabled).toBe(true)
  })

  it('全部可用时点击去结算调用 createOrder 并跳转订单详情', async () => {
    createOrder.mockResolvedValue({
      id: 60,
      orderNo: 'T20260901001',
      status: 'CREATED',
      totalAmount: 17.0,
    })
    const { wrapper, router } = await mountCart(AVAILABLE_CART)
    expect(wrapper.get('[data-testid="cart-checkout"]').element.disabled).toBe(false)
    await wrapper.get('[data-testid="cart-checkout"]').trigger('click')
    await flushPromises()
    expect(createOrder).toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/orders/60')
  })

  it('点击单条删除按钮调用 removeItem 并移除该行', async () => {
    removeItem.mockResolvedValue(null)
    const { wrapper } = await mountCart()
    await wrapper.get('[data-testid="cart-remove-50"]').trigger('click')
    await flushPromises()
    expect(removeItem).toHaveBeenCalledWith(50)
    expect(wrapper.find('[data-testid="cart-item-50"]').exists()).toBe(false)
  })

  it('修改数量失败时保留原数量并展示后端原因', async () => {
    updateItem.mockRejectedValue(new Error('库存不足'))
    const { wrapper } = await mountCart(AVAILABLE_CART)

    await wrapper.get('[data-testid="cart-plus-50"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="cart-qty-50"]').text()).toContain('2')
    expect(wrapper.text()).toContain('库存不足')
  })

  it.each(['无权限访问', '购物车不存在', '服务器异常'])(
    '加载失败“%s”时展示可测试错误状态',
    async (message) => {
      getCart.mockRejectedValue(new Error(message))
      const { wrapper } = await mountView(Cart, { path: '/cart' })
      await flushPromises()

      expect(wrapper.text()).toContain('无法加载购物车')
      expect(wrapper.get('[data-testid="cart-empty"]').exists()).toBe(true)
    },
  )

  it('下单请求未完成时禁用结算按钮', async () => {
    createOrder.mockReturnValue(new Promise(() => {}))
    const { wrapper } = await mountCart(AVAILABLE_CART)

    await wrapper.get('[data-testid="cart-checkout"]').trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.get('[data-testid="cart-checkout"]').element.disabled).toBe(true)
    expect(createOrder).toHaveBeenCalledTimes(1)
  })

  it('按FR-024采集收货人、联系电话和地址并提交快照', async () => {
    createOrder.mockResolvedValue({ id: 60, status: 'CREATED' })
    const { wrapper } = await mountCart(AVAILABLE_CART)

    await wrapper.get('.cart-drawer-bar').trigger('click')
    await wrapper.get('[data-testid="cart-recipient"]').setValue('张同学')
    await wrapper.get('[data-testid="cart-contact"]').setValue('02285356000')
    await wrapper.get('[data-testid="cart-address"]').setValue('天津大学北洋园校区学生宿舍1号楼')
    await wrapper.get('[data-testid="cart-checkout"]').trigger('click')
    await flushPromises()

    expect(createOrder).toHaveBeenCalledWith({
      shopId: 20,
      recipientName: '张同学',
      recipientPhone: '02285356000',
      deliveryAddress: '天津大学北洋园校区学生宿舍1号楼',
      saveToProfile: false,
    })
  })

  it('从本人资料预填收货信息但允许改成其他收货人', async () => {
    getProfile.mockResolvedValue({
      username: '下单用户',
      phone: '13800138000',
      address: '天津大学北洋园校区',
    })
    const { wrapper } = await mountCart(AVAILABLE_CART)
    await wrapper.get('.cart-drawer-bar').trigger('click')

    expect(wrapper.get('[data-testid="cart-recipient"]').element.value).toBe('下单用户')
    expect(wrapper.get('[data-testid="cart-contact"]').element.value).toBe('13800138000')

    await wrapper.get('[data-testid="cart-recipient"]').setValue('代收人')
    expect(wrapper.get('[data-testid="cart-recipient"]').element.value).toBe('代收人')
  })

  it('收货人、联系电话或地址为空时禁止提交', async () => {
    getProfile.mockResolvedValue({})
    const { wrapper } = await mountCart(AVAILABLE_CART)
    await wrapper.get('.cart-drawer-bar').trigger('click')

    expect(wrapper.get('.sheet-checkout').element.disabled).toBe(true)
    expect(createOrder).not.toHaveBeenCalled()
  })

  // NFR-006「错误说明可操作」：按钮灰掉时必须写清原因，不能只让用户看到"点了没反应"。
  it('结算按钮不可用时给出具体原因', async () => {
    getProfile.mockResolvedValue({ username: '下单用户', phone: '', address: '天津大学北洋园校区' })
    const { wrapper } = await mountCart(AVAILABLE_CART)
    await wrapper.get('.cart-drawer-bar').trigger('click')

    expect(wrapper.get('[data-testid="cart-checkout"]').element.disabled).toBe(true)
    expect(wrapper.get('[data-testid="cart-checkout-hint"]').text()).toContain('联系电话')

    await wrapper.get('[data-testid="cart-contact"]').setValue('13800138000')
    expect(wrapper.get('[data-testid="cart-checkout"]').element.disabled).toBe(false)
    expect(wrapper.find('[data-testid="cart-checkout-hint"]').exists()).toBe(false)
  })

  it('购物车里有不可用商品时说明是哪一类问题', async () => {
    const { wrapper } = await mountCart(FULL_CART)
    expect(wrapper.get('[data-testid="cart-checkout-hint"]').text()).toContain('已下架或缺货')
  })

  // EX-007：电话含空格、连字符或国际前缀时，规范化后 7–15 位可接受。
  it('联系电话含 + / 空格 / 连字符时规范化后提交', async () => {
    createOrder.mockResolvedValue({ id: 61, status: 'CREATED' })
    const { wrapper } = await mountCart(AVAILABLE_CART)

    await wrapper.get('.cart-drawer-bar').trigger('click')
    await wrapper.get('[data-testid="cart-recipient"]').setValue('张同学')
    await wrapper.get('[data-testid="cart-contact"]').setValue('+86 138-0013-8000')
    await wrapper.get('[data-testid="cart-address"]').setValue('天津大学北洋园校区学生宿舍1号楼')
    await wrapper.get('[data-testid="cart-checkout"]').trigger('click')
    await flushPromises()

    expect(createOrder).toHaveBeenCalledWith(expect.objectContaining({
      recipientPhone: '8613800138000',
    }))
  })

  it('联系电话规范化后不足 7 位、超长或非纯数字时禁止提交', async () => {
    getProfile.mockResolvedValue({})
    const { wrapper } = await mountCart(AVAILABLE_CART)

    await wrapper.get('.cart-drawer-bar').trigger('click')
    await wrapper.get('[data-testid="cart-recipient"]').setValue('张同学')
    await wrapper.get('[data-testid="cart-address"]').setValue('天津大学北洋园校区学生宿舍1号楼')

    for (const phone of ['-------+', '123456', '1234567890123456', '138001380a']) {
      await wrapper.get('[data-testid="cart-contact"]').setValue(phone)
      expect(wrapper.get('[data-testid="cart-checkout"]').element.disabled).toBe(true)
    }
    expect(createOrder).not.toHaveBeenCalled()
  })

  // maxlength 若小于 EX-007 允许的写法长度，国际前缀会被静默截断成错号码。
  it('联系电话输入上限不截断国际前缀写法', async () => {
    const { wrapper } = await mountCart(AVAILABLE_CART)
    await wrapper.get('.cart-drawer-bar').trigger('click')

    await wrapper.get('[data-testid="cart-contact"]').setValue('+86 138-0013-8000')
    expect(wrapper.get('[data-testid="cart-contact"]').element.value).toBe('+86 138-0013-8000')
  })
})
