// @vitest-environment jsdom
// 订单列表页红灯基线。mock 掉 api 模块；页面须提供 data-testid：
// order-row-<id> / orders-prev / orders-next / orders-empty。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/order', () => ({
  listOrders: vi.fn(),
  getOrder: vi.fn(),
  createOrder: vi.fn(),
  cancelOrder: vi.fn(),
  confirmOrder: vi.fn(),
}))

import { cancelOrder, confirmOrder, listOrders } from '@/api/order'
import { mountView } from '@/test/mountView'
import Orders from './Orders.vue'

const ORDER_A = {
  id: 11,
  orderNo: 'T20260901001',
  shopId: 7,
  totalAmount: 17.0,
  status: 'CREATED',
  paymentStatus: 'UNPAID',
  paymentDeadline: '2026-09-01T12:45:00',
  createdAt: '2026-09-01T12:30:00',
}
const ORDER_B = {
  id: 12,
  orderNo: 'T20260902002',
  shopId: 7,
  totalAmount: 6.0,
  status: 'CREATED',
  paymentStatus: 'UNPAID',
  paymentDeadline: '2026-09-02T08:15:00',
  createdAt: '2026-09-02T08:00:00',
}
const PAGE1 = { items: [ORDER_A, ORDER_B], page: 1, size: 20, total: 3, totalPages: 2 }
const PAGE2 = {
  items: [{ id: 13, orderNo: 'T20260903003', shopId: 7, totalAmount: 23.0, status: 'CREATED', createdAt: '2026-09-03T10:00:00' }],
  page: 2,
  size: 20,
  total: 3,
  totalPages: 2,
}
const EMPTY = { items: [], page: 1, size: 20, total: 0, totalPages: 0 }

// 页面会就地改写订单对象（取消/确认后写回 status）。这里深拷贝，避免某条用例的状态
// 泄漏到后续用例——之前 ORDER_A 被改成 CANCELLED 后，后面的用例全都拿到脏数据。
async function mountOrders(pageData = PAGE1) {
  listOrders.mockImplementation(async (query = {}) =>
    query.page === 2 ? structuredClone(PAGE2) : structuredClone(pageData),
  )
  const ctx = await mountView(Orders, { path: '/orders' })
  await flushPromises()
  return ctx
}

describe('订单列表页', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('加载后按列表展示订单号与金额', async () => {
    const { wrapper } = await mountOrders()
    expect(listOrders).toHaveBeenCalledWith({ page: 1, size: 20 })
    expect(wrapper.get('[data-testid="order-row-11"]').text()).toContain('T20260901001')
    expect(wrapper.get('[data-testid="order-row-11"]').text()).toContain('17.00')
    expect(wrapper.get('[data-testid="order-row-12"]').text()).toContain('T20260902002')
  })

  it('总页数大于 1 时点击下一页按第 2 页重新拉取', async () => {
    const { wrapper } = await mountOrders()
    expect(wrapper.get('[data-testid="orders-next"]').element.disabled).toBe(false)
    await wrapper.get('[data-testid="orders-next"]').trigger('click')
    await flushPromises()
    expect(listOrders).toHaveBeenCalledWith({ page: 2, size: 20 })
    expect(wrapper.get('[data-testid="order-row-13"]').text()).toContain('T20260903003')
  })

  it('第一页时「上一页」按钮禁用', async () => {
    const { wrapper } = await mountOrders()
    expect(wrapper.get('[data-testid="orders-prev"]').element.disabled).toBe(true)
  })

  it('点击某条订单跳转其详情', async () => {
    const { wrapper, router } = await mountOrders()
    await wrapper.get('[data-testid="order-row-11"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/orders/11')
  })

  it('没有订单时展示空状态', async () => {
    const { wrapper } = await mountOrders(EMPTY)
    expect(wrapper.get('[data-testid="orders-empty"]').text()).toContain('暂无订单')
  })

  it('选择状态筛选后按状态重新拉取订单', async () => {
    const { wrapper } = await mountOrders()
    await wrapper.get('[data-testid="orders-status"]').setValue('CREATED')
    await flushPromises()
    expect(listOrders).toHaveBeenLastCalledWith({ page: 1, size: 20, status: 'CREATED' })
  })

  it('待处理订单显示取消按钮，点击调用 cancelOrder 并标记已取消', async () => {
    cancelOrder.mockResolvedValue({ ...ORDER_A, status: 'CANCELLED' })
    const { wrapper } = await mountOrders()
    expect(wrapper.get('[data-testid="order-cancel-11"]').exists()).toBe(true)
    await wrapper.get('[data-testid="order-cancel-11"]').trigger('click')
    await flushPromises()
    expect(cancelOrder).toHaveBeenCalledWith(11)
    expect(wrapper.get('[data-testid="order-row-11"]').text()).toContain('已取消')
  })

  it('列表加载失败时展示可理解的错误状态', async () => {
    listOrders.mockRejectedValue(new Error('网络错误'))
    const { wrapper } = await mountView(Orders, { path: '/orders' })
    await flushPromises()

    expect(wrapper.text()).toContain('无法加载订单')
    expect(wrapper.get('[data-testid="orders-empty"]').text()).toContain('暂无订单')
  })

  it('已送达订单允许顾客确认收货并更新为已完成', async () => {
    const delivered = { ...ORDER_A, status: 'DELIVERED' }
    confirmOrder.mockResolvedValue({ ...delivered, status: 'COMPLETED' })
    const { wrapper } = await mountOrders({ ...PAGE1, items: [delivered] })

    await wrapper.get('[data-testid="order-confirm-11"]').trigger('click')
    await flushPromises()

    expect(confirmOrder).toHaveBeenCalledWith(11)
    expect(wrapper.get('[data-testid="order-row-11"]').text()).toContain('已完成')
  })

  it('已接单但骑手未送达时，确认按钮保留但禁用且不发请求', async () => {
    const accepted = { ...ORDER_A, status: 'ACCEPTED' }
    const { wrapper } = await mountOrders({ ...PAGE1, items: [accepted] })

    const button = wrapper.get('[data-testid="order-confirm-11"]')
    expect(button.element.disabled).toBe(true)
    expect(wrapper.get('[data-testid="order-row-11"]').text()).toContain('骑手送达后可确认')

    await button.trigger('click')
    await flushPromises()
    expect(confirmOrder).not.toHaveBeenCalled()
  })

  it('配送中／已送达订单展示中文状态', async () => {
    const rows = [
      { ...ORDER_A, id: 21, status: 'DELIVERING' },
      { ...ORDER_B, id: 22, status: 'DELIVERED' },
    ]
    const { wrapper } = await mountOrders({ ...PAGE1, items: rows })
    expect(wrapper.get('[data-testid="order-row-21"]').text()).toContain('配送中')
    expect(wrapper.get('[data-testid="order-row-22"]').text()).toContain('已送达')
  })

  // FR-016：顾客订单按分页、状态、时间查询。
  it('状态筛选项覆盖 §5.1 状态机全部取值，且不含 PENDING', async () => {
    const { wrapper } = await mountOrders()
    const values = wrapper.findAll('[data-testid="orders-status"] option')
      .map((option) => option.element.value)

    expect(values).toEqual(['', 'CREATED', 'CANCELLED', 'ACCEPTED', 'DELIVERING', 'DELIVERED', 'COMPLETED'])
    expect(values).not.toContain('PENDING')
  })

  it('填写起止时间后带参数重新拉取订单', async () => {
    const { wrapper } = await mountOrders()

    await wrapper.get('[data-testid="orders-start"]').setValue('2026-09-01T00:00')
    await flushPromises()
    expect(listOrders).toHaveBeenLastCalledWith({
      page: 1, size: 20, startTime: '2026-09-01T00:00',
    })

    await wrapper.get('[data-testid="orders-end"]').setValue('2026-09-02T00:00')
    await flushPromises()
    expect(listOrders).toHaveBeenLastCalledWith({
      page: 1, size: 20, startTime: '2026-09-01T00:00', endTime: '2026-09-02T00:00',
    })
  })

  // §5.1：CREATED+UNPAID 与 CREATED+PAID 允许的动作不同，取消只对前者开放。
  it('已支付但未接单的订单不再显示取消按钮', async () => {
    const paid = { ...ORDER_A, paymentStatus: 'PAID' }
    const { wrapper } = await mountOrders({ ...PAGE1, items: [paid] })

    expect(wrapper.find('[data-testid="order-cancel-11"]').exists()).toBe(false)
    expect(wrapper.get('[data-testid="order-paid-hint-11"]').text()).toContain('已支付')
    expect(cancelOrder).not.toHaveBeenCalled()
  })

  // §9.1 / FR-019：倒计时必须用服务器给的 paymentDeadline，不能拿 createdAt 自己加 15 分钟。
  it('倒计时以服务器 paymentDeadline 为准', async () => {
    const { wrapper } = await mountOrders({
      ...PAGE1,
      // createdAt 已过 15 分钟，但服务器给的截止时间还没到 —— 本地推算会误报"已超时"
      items: [{ ...ORDER_A, createdAt: '2026-09-01T12:00:00', paymentDeadline: '2099-01-01T00:00:00' }],
    })

    const text = wrapper.get('[data-testid="order-row-11"]').text()
    expect(text).toContain('距自动取消')
    expect(text).not.toContain('已超时')
  })

  it('已支付订单不显示任何自动取消倒计时', async () => {
    const { wrapper } = await mountOrders({
      ...PAGE1,
      items: [{ ...ORDER_A, paymentStatus: 'PAID', paymentDeadline: '2020-01-01T00:00:00' }],
    })

    expect(wrapper.get('[data-testid="order-row-11"]').text()).not.toContain('距自动取消')
    expect(wrapper.get('[data-testid="order-row-11"]').text()).not.toContain('已超时')
  })

  // §5.1 状态机没有 PENDING，订单页不应再为它保留分支。
  it('PENDING 不再被当作待处理状态展示', async () => {
    const { wrapper } = await mountOrders({ ...PAGE1, items: [{ ...ORDER_A, id: 31, status: 'PENDING' }] })

    expect(wrapper.get('[data-testid="order-row-31"]').text()).toContain('PENDING')
    expect(wrapper.find('[data-testid="order-cancel-31"]').exists()).toBe(false)
  })
})
