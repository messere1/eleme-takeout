// @vitest-environment jsdom
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/user', () => ({ listUsers: vi.fn() }))
vi.mock('@/api/merchant', () => ({ listMerchants: vi.fn() }))

import { listUsers } from '@/api/user'
import { listMerchants } from '@/api/merchant'
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

  it('默认按20条加载用户并只展示只读字段', async () => {
    const { wrapper } = await mountAdmin([
      { id: 7, username: 'alice', phone: '138****8000', nickname: 'Alice' },
    ])

    expect(listUsers).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('alice')
    expect(wrapper.text()).toContain('138****8000')
    expect(wrapper.text()).not.toMatch(/删除|封禁|冒充|修改/)
  })

  it('切换商家页签后加载商家清单', async () => {
    listMerchants.mockResolvedValue({
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
      expect(wrapper.text()).toContain('管理接口暂不可用')
      expect(wrapper.find('section.admin').exists()).toBe(true)
    }
  })

  it('即使接口误返完整手机号页面也不得直接暴露', async () => {
    const { wrapper } = await mountAdmin([
      { id: 7, username: 'alice', phone: '13800138000', nickname: 'Alice' },
    ])
    expect(wrapper.text()).not.toContain('13800138000')
  })
})
