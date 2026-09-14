// @vitest-environment jsdom
// 订单详情页红灯基线。mock 掉 api 模块；页面须提供 data-testid：
// order-no / order-status / order-total / order-item-<productId> / order-back。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/order', () => ({ getOrder: vi.fn(), listOrders: vi.fn(), createOrder: vi.fn(), requestRefund: vi.fn() }))

import { getOrder, requestRefund } from '@/api/order'
import { mountView } from '@/test/mountView'
import OrderDetail from './OrderDetail.vue'

const DETAIL = {
  id: 11,
  orderNo: 'T20260901001',
  shopId: 7,
  totalAmount: 17.0,
  status: 'CREATED',
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
    expect(wrapper.get('[data-testid="order-status"]').text()).toContain('CREATED')
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

  // §5.1：CREATED+UNPAID 才允许顾客支付。
  it('未支付订单提供支付入口', async () => {
    getOrder.mockResolvedValue({ ...DETAIL, status: 'CREATED', paymentStatus: 'UNPAID' })
    const { wrapper } = await mountView(OrderDetail, { path: '/orders/11' })
    await flushPromises()

    expect(wrapper.find('.pay-link').exists()).toBe(true)
  })

  it('已支付但商家尚未接单的订单（status 仍是 CREATED）不再给支付入口', async () => {
    // 后端支付只改 paymentStatus，status 保持 CREATED 直到商家接单，
    // 只看 status 会把已支付订单再次引导去付款。
    getOrder.mockResolvedValue({ ...DETAIL, status: 'CREATED', paymentStatus: 'PAID' })
    const { wrapper } = await mountView(OrderDetail, { path: '/orders/11' })
    await flushPromises()

    expect(wrapper.find('.pay-link').exists()).toBe(false)
    expect(wrapper.get('[data-testid="order-paid-tip"]').text()).toContain('已支付')
  })

  it('已支付订单不可再把状态走到 PENDING 分支', async () => {
    // §5.1 状态机里没有 PENDING，不应再有依赖它的入口。
    getOrder.mockResolvedValue({ ...DETAIL, status: 'PENDING', paymentStatus: 'UNPAID' })
    const { wrapper } = await mountView(OrderDetail, { path: '/orders/11' })
    await flushPromises()

    expect(wrapper.find('.pay-link').exists()).toBe(false)
  })

  describe('申请退款（UC-06 / §9.1）', () => {
    async function mountPaid() {
      getOrder.mockResolvedValue({ ...DETAIL, paymentStatus: 'PAID' })
      const ctx = await mountView(OrderDetail, { path: '/orders/11' })
      await flushPromises()
      return ctx
    }
    const submitDisabled = (wrapper) =>
      wrapper.get('[data-testid="refund-submit"]').element.disabled

    it('金额为三位小数、指数形式或零时禁止提交', async () => {
      const { wrapper } = await mountPaid()
      await wrapper.get('[data-testid="refund-reason"]').setValue('漏放商品')

      for (const amount of ['1.234', '1e-7', '0', '-1', '007']) {
        await wrapper.get('[data-testid="refund-amount"]').setValue(amount)
        expect(submitDisabled(wrapper)).toBe(true)
      }
      expect(wrapper.get('[data-testid="refund-hint"]').text()).toContain('0.01～99999999.99')
      expect(requestRefund).not.toHaveBeenCalled()
    })

    it('原因为空时禁止提交', async () => {
      const { wrapper } = await mountPaid()
      await wrapper.get('[data-testid="refund-amount"]').setValue('12.50')

      expect(submitDisabled(wrapper)).toBe(true)
      await wrapper.get('[data-testid="refund-reason"]').setValue('漏放商品')
      expect(submitDisabled(wrapper)).toBe(false)
    })

    it('证据超过 3 个时禁止提交', async () => {
      const { wrapper } = await mountPaid()
      await wrapper.get('[data-testid="refund-amount"]').setValue('12.50')
      await wrapper.get('[data-testid="refund-reason"]').setValue('漏放商品')
      await wrapper.get('[data-testid="refund-evidence"]')
        .setValue('/uploads/1.jpg\n/uploads/2.jpg\n/uploads/3.jpg')

      expect(submitDisabled(wrapper)).toBe(false)
      await wrapper.get('[data-testid="refund-evidence"]')
        .setValue('/uploads/1.jpg\n/uploads/2.jpg\n/uploads/3.jpg\n/uploads/4.jpg')
      expect(submitDisabled(wrapper)).toBe(true)
      expect(wrapper.get('[data-testid="refund-hint"]').text()).toContain('最多 3')
    })

    it('合法提交时按契约发送数字金额与证据 URL', async () => {
      requestRefund.mockResolvedValue({})
      const { wrapper } = await mountPaid()
      await wrapper.get('[data-testid="refund-amount"]').setValue('12.50')
      await wrapper.get('[data-testid="refund-reason"]').setValue('漏放商品')
      await wrapper.get('[data-testid="refund-evidence"]')
        .setValue('/uploads/evidence-1.jpg\n\n/uploads/evidence-2.jpg')

      await wrapper.get('[data-testid="refund-submit"]').trigger('click')
      await flushPromises()

      expect(requestRefund).toHaveBeenCalledWith(11, {
        amount: 12.5,
        reason: '漏放商品',
        evidenceUrls: ['/uploads/evidence-1.jpg', '/uploads/evidence-2.jpg'],
      })
      expect(wrapper.text()).toContain('退款申请已提交')
    })

    // NFR-006 / EX-012：提交期间禁用，重复点击不再发请求。
    it('提交期间禁用按钮且只发一次请求', async () => {
      requestRefund.mockReturnValue(new Promise(() => {}))
      const { wrapper } = await mountPaid()
      await wrapper.get('[data-testid="refund-amount"]').setValue('12.50')
      await wrapper.get('[data-testid="refund-reason"]').setValue('漏放商品')

      await wrapper.get('[data-testid="refund-submit"]').trigger('click')
      await wrapper.vm.$nextTick()

      expect(submitDisabled(wrapper)).toBe(true)
      expect(wrapper.get('[data-testid="refund-submit"]').text()).toContain('提交中')
      expect(requestRefund).toHaveBeenCalledTimes(1)
    })
  })
})
