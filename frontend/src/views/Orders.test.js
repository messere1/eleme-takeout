// @vitest-environment jsdom
// 订单列表页红灯基线。mock 掉 api 模块；页面须提供 data-testid：
// order-row-<id> / orders-prev / orders-next / orders-empty。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/order', () => ({ listOrders: vi.fn(), getOrder: vi.fn(), createOrder: vi.fn() }))

import { listOrders } from '@/api/order'
import { mountView } from '@/test/mountView'
import Orders from './Orders.vue'

const ORDER_A = {
  id: 11,
  orderNo: 'T20260901001',
  shopId: 7,
  totalAmount: 17.0,
  status: 'PENDING',
  createdAt: '2026-09-01T12:30:00',
}
const ORDER_B = {
  id: 12,
  orderNo: 'T20260902002',
  shopId: 7,
  totalAmount: 6.0,
  status: 'PENDING',
  createdAt: '2026-09-02T08:00:00',
}
const PAGE1 = { items: [ORDER_A, ORDER_B], page: 1, size: 10, total: 3, totalPages: 2 }
const PAGE2 = {
  items: [{ id: 13, orderNo: 'T20260903003', shopId: 7, totalAmount: 23.0, status: 'PENDING', createdAt: '2026-09-03T10:00:00' }],
  page: 2,
  size: 10,
  total: 3,
  totalPages: 2,
}
const EMPTY = { items: [], page: 1, size: 10, total: 0, totalPages: 0 }

async function mountOrders(pageData = PAGE1) {
  listOrders.mockImplementation(async (query = {}) =>
    query.page === 2 ? PAGE2 : pageData,
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
    expect(listOrders).toHaveBeenCalledWith({ page: 1, size: 10 })
    expect(wrapper.get('[data-testid="order-row-11"]').text()).toContain('T20260901001')
    expect(wrapper.get('[data-testid="order-row-11"]').text()).toContain('17.00')
    expect(wrapper.get('[data-testid="order-row-12"]').text()).toContain('T20260902002')
  })

  it('总页数大于 1 时点击下一页按第 2 页重新拉取', async () => {
    const { wrapper } = await mountOrders()
    expect(wrapper.get('[data-testid="orders-next"]').element.disabled).toBe(false)
    await wrapper.get('[data-testid="orders-next"]').trigger('click')
    await flushPromises()
    expect(listOrders).toHaveBeenCalledWith({ page: 2, size: 10 })
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
    await wrapper.get('[data-testid="orders-status"]').setValue('PENDING')
    await flushPromises()
    expect(listOrders).toHaveBeenLastCalledWith({ page: 1, size: 10, status: 'PENDING' })
  })
})
