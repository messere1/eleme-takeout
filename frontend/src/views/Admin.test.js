// @vitest-environment jsdom
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/user', () => ({ listUsers: vi.fn() }))
vi.mock('@/api/merchant', () => ({ listMerchants: vi.fn() }))
vi.mock('@/api/admin', () => ({ listAdminProducts: vi.fn(), listAdminOrders: vi.fn(), setAdminStatus: vi.fn(), listAdminRefunds: vi.fn(), decideRefund: vi.fn() }))

import { listUsers } from '@/api/user'
import { listMerchants } from '@/api/merchant'
import { listAdminOrders, setAdminStatus } from '@/api/admin'
import { mountView } from '@/test/mountView'
import Admin from './Admin.vue'

async function mountAdmin(users = []) {
  listUsers.mockResolvedValue({ items: users, page: 1, size: 20, total: users.length })
  listMerchants.mockResolvedValue({ items: [], page: 1, size: 20, total: 0 })
  const context = await mountView(Admin, { path: '/admin' })
  await flushPromises()
  return context
}

describe('管理员只读账号清单', () => {
  beforeEach(() => vi.clearAllMocks())

  it('默认按20条加载用户并脱敏展示', async () => {
    const { wrapper } = await mountAdmin([
      { id: 7, username: 'alice', phone: '138****8000', nickname: 'Alice' },
    ])

    expect(listUsers).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('alice')
    expect(wrapper.text()).toContain('138****8000')
  })

  it('切换商家页签后加载商家清单', async () => {
    listMerchants.mockResolvedValueOnce({
      items: [{ id: 12, merchantName: '北洋餐厅', phone: '139****9000', businessScope: '快餐' }],
    })
    const { wrapper } = await mountAdmin()

    await wrapper.findAll('button')[1].trigger('click')
    await flushPromises()

    expect(listMerchants).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('北洋餐厅')
  })

  it('接口未完成时显示加载状态', async () => {
    listUsers.mockReturnValue(new Promise(() => {}))
    const { wrapper } = await mountView(Admin, { path: '/admin' })
    expect(wrapper.get('[data-testid="admin-loading"]').text()).toContain('加载')
  })

  it('空用户清单显示明确空状态', async () => {
    const { wrapper } = await mountAdmin()
    expect(wrapper.get('[data-testid="admin-empty"]').text()).toContain('暂无')
  })

  it('403和500均显示可理解错误且不白屏', async () => {
    for (const message of ['无权访问', '服务器内部错误']) {
      listUsers.mockRejectedValueOnce(new Error(message))
      const { wrapper } = await mountView(Admin, { path: '/admin' })
      await flushPromises()
      expect(wrapper.text()).toContain(message)
      expect(wrapper.find('section.admin').exists()).toBe(true)
    }
  })

  it('即使接口误返完整手机号页面也不得直接暴露', async () => {
    const { wrapper } = await mountAdmin([
      { id: 7, username: 'alice', phone: '13800138000', nickname: 'Alice' },
    ])
    expect(wrapper.text()).not.toContain('13800138000')
  })

  it('管理员可把订单状态改为配送中', async () => {
    listAdminOrders.mockResolvedValue({
      items: [{ id: 9, orderNo: 'NO-9', status: 'CREATED', paymentStatus: 'UNPAID' }],
    })
    setAdminStatus.mockResolvedValue(null)
    const { wrapper } = await mountAdmin()

    await wrapper.findAll('button')[3].trigger('click') // 订单页签
    await flushPromises()
    expect(wrapper.get('[data-testid="admin-order-status-9"]').text()).toContain('待处理')

    await wrapper.get('[data-testid="admin-order-target-9"]').setValue('DELIVERING')
    await wrapper.get('[data-testid="admin-order-set-9"]').trigger('click')
    await flushPromises()

    expect(setAdminStatus).toHaveBeenCalledWith('orders', 9, 'DELIVERING')
    expect(wrapper.get('[data-testid="admin-order-status-9"]').text()).toContain('配送中')
  })

  // FR-016：管理员订单列表也要能按状态、时间查询。
  it('订单页签支持按状态与起止时间筛选', async () => {
    listAdminOrders.mockResolvedValue({ items: [] })
    const { wrapper } = await mountAdmin()

    await wrapper.findAll('button')[3].trigger('click') // 订单页签
    await flushPromises()
    expect(listAdminOrders).toHaveBeenLastCalledWith({ page: 1, size: 20 })

    await wrapper.get('[data-testid="admin-order-status-filter"]').setValue('DELIVERING')
    await flushPromises()
    expect(listAdminOrders).toHaveBeenLastCalledWith({ page: 1, size: 20, status: 'DELIVERING' })

    await wrapper.get('[data-testid="admin-order-start"]').setValue('2026-09-01T00:00')
    await flushPromises()
    expect(listAdminOrders).toHaveBeenLastCalledWith({
      page: 1, size: 20, status: 'DELIVERING', startTime: '2026-09-01T00:00',
    })
  })

  it('订单状态更新失败时展示后端原因且列表状态不变', async () => {
    listAdminOrders.mockResolvedValue({
      items: [{ id: 9, orderNo: 'NO-9', status: 'CREATED', paymentStatus: 'UNPAID' }],
    })
    setAdminStatus.mockRejectedValue(new Error('订单状态不合法'))
    const { wrapper } = await mountAdmin()

    await wrapper.findAll('button')[3].trigger('click')
    await flushPromises()

    await wrapper.get('[data-testid="admin-order-target-9"]').setValue('CANCELLED')
    await wrapper.get('[data-testid="admin-order-set-9"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="admin-error"]').text()).toContain('订单状态不合法')
    expect(wrapper.get('[data-testid="admin-order-status-9"]').text()).toContain('待处理')
  })
})
