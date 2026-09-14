// @vitest-environment jsdom
// 商家订单管理页红灯基线。mock 掉 api 模块；页面须提供 data-testid：
// order-mgmt-item-<id> / order-mgmt-accept-<id> / order-mgmt-complete-<id> / order-mgmt-empty。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/order', () => ({
  listMerchantOrders: vi.fn(),
  acceptOrder: vi.fn(),
  listMerchantRefunds: vi.fn().mockResolvedValue([]),
  decideMerchantRefund: vi.fn(),
}))

import { acceptOrder, listMerchantOrders } from '@/api/order'
import { session } from '@/utils/session'
import { mountView } from '@/test/mountView'
import MerchantOrders from './MerchantOrders.vue'

// §5.1 状态机里没有 PENDING，待接单就是 CREATED；FR-020 要求已支付才能接单。
const ORDERS = [
  { id: 60, orderNo: 'T20260901001', shopId: 7, totalAmount: 17.0, status: 'CREATED', paymentStatus: 'PAID', createdAt: '2026-09-01T12:30:00' },
  { id: 61, orderNo: 'T20260902002', shopId: 7, totalAmount: 23.0, status: 'ACCEPTED', createdAt: '2026-09-02T10:00:00' },
]

async function mountOrders(data = ORDERS) {
  session.save({ token: 'mt-1', role: 'MERCHANT' })
  session.saveShop({ merchantId: 12, shopId: 7, shopName: '北洋餐厅' })
  // 页面接单后会就地改写 order.status，深拷贝避免污染共用的 ORDERS 夹具
  listMerchantOrders.mockResolvedValue(structuredClone(data))
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

  // FR-020 / EX-016：未支付订单不能接单，按钮不该出现。
  it('未支付订单不提供接单按钮，改为提示等待支付', async () => {
    const unpaid = { ...ORDERS[0], id: 63, orderNo: 'T20260904004', paymentStatus: 'UNPAID' }
    const { wrapper } = await mountOrders([unpaid])

    expect(wrapper.find('[data-testid="order-mgmt-accept-63"]').exists()).toBe(false)
    expect(wrapper.get('[data-testid="order-mgmt-unpaid-63"]').text()).toContain('等待顾客支付')
  })

  it('商家接单调用 acceptOrder 并更新为已接单', async () => {
    acceptOrder.mockResolvedValue({ ...ORDERS[0], status: 'ACCEPTED' })
    const { wrapper } = await mountOrders()
    await wrapper.get('[data-testid="order-mgmt-accept-60"]').trigger('click')
    await flushPromises()
    expect(acceptOrder).toHaveBeenCalledWith(60)
    expect(wrapper.get('[data-testid="order-mgmt-item-60"]').text()).toContain('已接单')
  })

  it('商家不能直接完成订单：点击只说明新流程，不触发任何订单接口', async () => {
    const { wrapper } = await mountOrders()
    await wrapper.get('[data-testid="order-mgmt-complete-61"]').trigger('click')
    await flushPromises()
    // §5.1 完成路径是「DELIVERED → 顾客确认收货」（FR-022），§9 接口表也没有商家完成的接口
    expect(acceptOrder).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('顾客确认收货')
    expect(wrapper.get('[data-testid="order-mgmt-item-61"]').text()).toContain('已接单')
  })

  it('配送中／已送达订单展示中文状态', async () => {
    const { wrapper } = await mountOrders([
      { id: 62, orderNo: 'T20260903003', shopId: 7, totalAmount: 9.0, status: 'DELIVERING', createdAt: '2026-09-03T10:00:00' },
      { id: 63, orderNo: 'T20260903004', shopId: 7, totalAmount: 12.0, status: 'DELIVERED', createdAt: '2026-09-03T11:00:00' },
    ])
    expect(wrapper.get('[data-testid="order-mgmt-item-62"]').text()).toContain('配送中')
    expect(wrapper.get('[data-testid="order-mgmt-item-63"]').text()).toContain('已送达')
  })

  // FR-016：商家订单按分页、状态、时间查询（此前完全不传参数）。
  it('商家订单带分页与状态、时间参数请求', async () => {
    const { wrapper } = await mountOrders()
    expect(listMerchantOrders).toHaveBeenCalledWith({ page: 1, size: 20 })

    await wrapper.get('[data-testid="order-mgmt-status"]').setValue('ACCEPTED')
    await flushPromises()
    expect(listMerchantOrders).toHaveBeenLastCalledWith({ page: 1, size: 20, status: 'ACCEPTED' })

    await wrapper.get('[data-testid="order-mgmt-start"]').setValue('2026-09-01T00:00')
    await flushPromises()
    expect(listMerchantOrders).toHaveBeenLastCalledWith({
      page: 1, size: 20, status: 'ACCEPTED', startTime: '2026-09-01T00:00',
    })
  })

  it('状态筛选项覆盖 §5.1 状态机全部取值，且不含 PENDING', async () => {
    const { wrapper } = await mountOrders()
    const values = wrapper.findAll('[data-testid="order-mgmt-status"] option')
      .map((option) => option.element.value)

    expect(values).toEqual(['', 'CREATED', 'ACCEPTED', 'DELIVERING', 'DELIVERED', 'COMPLETED', 'CANCELLED'])
    expect(values).not.toContain('PENDING')
  })

  it('多页时按页重新拉取', async () => {
    session.save({ token: 'mt-1', role: 'MERCHANT' })
    session.saveShop({ merchantId: 12, shopId: 7, shopName: '北洋餐厅' })
    const page1 = { items: ORDERS, page: 1, size: 20, total: 2, totalPages: 2 }
    const page2 = {
      items: [{ ...ORDERS[1], id: 62, orderNo: 'T20260903003' }],
      page: 2,
      size: 20,
      total: 2,
      totalPages: 2,
    }
    listMerchantOrders.mockImplementation(async (query) => (query?.page === 2 ? page2 : page1))

    const { wrapper } = await mountView(MerchantOrders, { path: '/merchant/orders' })
    await flushPromises()

    expect(wrapper.get('[data-testid="order-mgmt-next"]').element.disabled).toBe(false)
    await wrapper.get('[data-testid="order-mgmt-next"]').trigger('click')
    await flushPromises()

    expect(listMerchantOrders).toHaveBeenLastCalledWith({ page: 2, size: 20 })
    expect(wrapper.get('[data-testid="order-mgmt-item-62"]').exists()).toBe(true)
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
