// @vitest-environment jsdom
// 收银台。倒计时必须以服务器 paymentDeadline 为准（§9.1 / FR-019），
// 且 paymentDeadline 缺失的历史订单不能被本地误判成"已超时"。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/order', () => ({ getOrder: vi.fn(), payOrder: vi.fn() }))

import { getOrder, payOrder } from '@/api/order'
import { mountView } from '@/test/mountView'
import Pay from './Pay.vue'

const ORDER = {
  id: 11,
  orderNo: 'T20260901001',
  totalAmount: 17.0,
  status: 'CREATED',
  paymentStatus: 'UNPAID',
  paymentDeadline: '2099-01-01T00:00:00',
}

async function mountPay(overrides = {}) {
  getOrder.mockResolvedValue({ ...ORDER, ...overrides })
  const ctx = await mountView(Pay, { path: '/orders/11/pay' })
  await flushPromises()
  return ctx
}

describe('收银台', () => {
  beforeEach(() => vi.clearAllMocks())

  it('按服务器截止时间倒计时并允许支付', async () => {
    const { wrapper } = await mountPay()

    expect(wrapper.text()).toContain('内完成支付')
    expect(wrapper.get('[data-testid="pay-submit"]').element.disabled).toBe(false)
  })

  it('超过服务器截止时间后禁用支付', async () => {
    const { wrapper } = await mountPay({ paymentDeadline: '2000-01-01T00:00:00' })

    expect(wrapper.get('[data-testid="pay-submit"]').element.disabled).toBe(true)
  })

  // 历史订单的 paymentDeadline 可能为 null。以前用 new Date(0) 顶替，倒计时恒为 00:00、
  // 按钮永久禁用，还会被本地定时器标成 CANCELLED，订单再也付不了。
  it('paymentDeadline 为空时不本地判定超时，仍允许提交给后端裁定', async () => {
    const { wrapper } = await mountPay({ paymentDeadline: null })

    expect(wrapper.text()).not.toContain('00:00')
    expect(wrapper.get('[data-testid="pay-submit"]').element.disabled).toBe(false)
  })

  it('支付成功后跳转订单详情', async () => {
    payOrder.mockResolvedValue({ ...ORDER, paymentStatus: 'PAID' })
    const { wrapper, router } = await mountPay()

    await wrapper.get('[data-testid="pay-submit"]').trigger('click')
    await flushPromises()

    expect(payOrder).toHaveBeenCalledWith(11)
    expect(router.currentRoute.value.path).toBe('/orders/11')
  })

  // EX-012：支付期间防重复。
  it('支付请求未完成时禁用按钮且只发一次', async () => {
    payOrder.mockReturnValue(new Promise(() => {}))
    const { wrapper } = await mountPay()

    await wrapper.get('[data-testid="pay-submit"]').trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.get('[data-testid="pay-submit"]').element.disabled).toBe(true)
    expect(payOrder).toHaveBeenCalledTimes(1)
  })

  it('订单加载失败时展示错误而不是永久加载', async () => {
    getOrder.mockRejectedValue(new Error('订单不存在'))
    const { wrapper } = await mountView(Pay, { path: '/orders/11/pay' })
    await flushPromises()

    expect(wrapper.text()).toContain('无法加载订单')
  })
})
