// @vitest-environment jsdom
// 商家订单管理页红灯基线。mock 掉 api 模块；页面须提供 data-testid：
// order-mgmt-item-<id> / order-mgmt-accept-<id> / order-mgmt-complete-<id> / order-mgmt-empty。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/order', () => ({
  listMerchantOrders: vi.fn(),
  acceptOrder: vi.fn(),
  completeOrder: vi.fn(),
  listMerchantRefunds: vi.fn().mockResolvedValue([]),
  decideMerchantRefund: vi.fn(),
}))

import { acceptOrder, completeOrder, listMerchantOrders } from '@/api/order'
import { session } from '@/utils/session'
import { mountView } from '@/test/mountView'
import MerchantOrders from './MerchantOrders.vue'

const ORDERS = [
  { id: 60, orderNo: 'T20260901001', shopId: 7, totalAmount: 17.0, status: 'PENDING', createdAt: '2026-09-01T12:30:00' },
  { id: 61, orderNo: 'T20260902002', shopId: 7, totalAmount: 23.0, status: 'ACCEPTED', createdAt: '2026-09-02T10:00:00' },
]

async function mountOrders(data = ORDERS) {
  session.save({ token: 'mt-1', role: 'MERCHANT' })
  session.saveShop({ merchantId: 12, shopId: 7, shopName: '北洋餐厅' })
  listMerchantOrders.mockResolvedValue(data)
  const ctx = await mountView(MerchantOrders, { path: '/merchant/orders' })
  await flushPromises()
  return ctx
}

describe('商家订单管理', () => {
  beforeEach(() => {
    session.clear()
    vi.clearAllMocks()
  })

  it('展示本人店铺订单及可执行动作', async () => {
    const { wrapper } = await mountOrders()
    expect(listMerchantOrders).toHaveBeenCalled()
    const pending = wrapper.get('[data-testid="order-mgmt-item-60"]')
    expect(pending.text()).toContain('T20260901001')
    expect(pending.text()).toContain('待接单')
    expect(wrapper.get('[data-testid="order-mgmt-accept-60"]').exists()).toBe(true)
    // 已接单订单提供“完成”，不提供“接单”
    expect(wrapper.get('[data-testid="order-mgmt-complete-61"]').exists()).toBe(true)
  })

  it('商家接单调用 acceptOrder 并更新为已接单', async () => {
    acceptOrder.mockResolvedValue({ ...ORDERS[0], status: 'ACCEPTED' })
    const { wrapper } = await mountOrders()
    await wrapper.get('[data-testid="order-mgmt-accept-60"]').trigger('click')
    await flushPromises()
    expect(acceptOrder).toHaveBeenCalledWith(60)
    expect(wrapper.get('[data-testid="order-mgmt-item-60"]').text()).toContain('已接单')
  })

  it('商家完成订单调用 completeOrder 并更新为已完成', async () => {
    completeOrder.mockResolvedValue({ ...ORDERS[1], status: 'COMPLETED' })
    const { wrapper } = await mountOrders()
    await wrapper.get('[data-testid="order-mgmt-complete-61"]').trigger('click')
    await flushPromises()
    expect(completeOrder).toHaveBeenCalledWith(61)
    expect(wrapper.get('[data-testid="order-mgmt-item-61"]').text()).toContain('已完成')
  })

  it('没有订单时展示空状态', async () => {
    const { wrapper } = await mountOrders([])
    expect(wrapper.get('[data-testid="order-mgmt-empty"]').text()).toContain('暂无订单')
  })

  it.each(['无权限访问', '订单资源不存在', '服务器异常'])(
    '列表加载失败“%s”时展示用户可读状态',
    async (message) => {
      listMerchantOrders.mockRejectedValue(new Error(message))
      session.save({ token: 'mt-1', role: 'MERCHANT' })
      const { wrapper } = await mountView(MerchantOrders, { path: '/merchant/orders' })
      await flushPromises()

      expect(wrapper.text()).toContain(message)
      expect(wrapper.get('[data-testid="order-mgmt-empty"]').exists()).toBe(true)
    },
  )
})
