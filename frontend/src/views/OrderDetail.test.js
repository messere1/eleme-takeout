// @vitest-environment jsdom
// 订单详情页红灯基线。mock 掉 api 模块；页面须提供 data-testid：
// order-no / order-status / order-total / order-item-<productId> / order-back。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/order', () => ({ getOrder: vi.fn(), listOrders: vi.fn(), createOrder: vi.fn() }))

import { getOrder } from '@/api/order'
import { mountView } from '@/test/mountView'
import OrderDetail from './OrderDetail.vue'

const DETAIL = {
  id: 11,
  orderNo: 'T20260901001',
  shopId: 7,
  totalAmount: 17.0,
  status: 'PENDING',
  createdAt: '2026-09-01T12:30:00',
  items: [
    { productId: 40, productName: '煎饼果子', unitPrice: 8.5, quantity: 2, subtotal: 17.0 },
    { productId: 44, productName: '柠檬茶', unitPrice: 6.0, quantity: 1, subtotal: 6.0 },
  ],
}

async function mountDetail() {
  getOrder.mockResolvedValue(DETAIL)
  const ctx = await mountView(OrderDetail, { path: '/orders/11' })
  await flushPromises()
  return ctx
}

describe('订单详情页', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('展示订单号、状态与总金额', async () => {
    const { wrapper } = await mountDetail()
    expect(getOrder).toHaveBeenCalledWith(11)
    expect(wrapper.get('[data-testid="order-no"]').text()).toContain('T20260901001')
    expect(wrapper.get('[data-testid="order-status"]').text()).toContain('PENDING')
    expect(wrapper.get('[data-testid="order-total"]').text()).toContain('17.00')
  })

  it('逐条展示商品行与金额', async () => {
    const { wrapper } = await mountDetail()
    const first = wrapper.get('[data-testid="order-item-40"]')
    expect(first.text()).toContain('煎饼果子')
    expect(first.text()).toContain('17.00')
    expect(wrapper.get('[data-testid="order-item-44"]').text()).toContain('柠檬茶')
  })

  it('详情加载失败时展示错误状态而不是永久加载', async () => {
    getOrder.mockRejectedValue(new Error('无权访问'))
    const { wrapper } = await mountView(OrderDetail, { path: '/orders/11' })
    await flushPromises()

    expect(wrapper.text()).toContain('无法加载订单详情')
    expect(wrapper.text()).not.toContain('订单加载中')
  })
})
