// @vitest-environment jsdom
// 首页店铺流 红灯基线。mock 掉 api 模块；页面须提供 data-testid：
// home-shop-feed / home-shop-<id> / home-feed-note（示例/离线提示）。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/shop', () => ({ listShops: vi.fn() }))

import { listShops } from '@/api/shop'
import { mountView } from '@/test/mountView'
import Home from './Home.vue'

const SHOPS = [
  { id: 1, shopName: '北洋餐厅', notice: '煎饼果子现做现卖', status: 'OPEN' },
  { id: 2, shopName: '川渝小馆', notice: '麻辣鲜香', status: 'OPEN' },
]

async function mountHome() {
  const ctx = await mountView(Home, { path: '/' })
  await flushPromises()
  return ctx
}

describe('首页店铺流', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('列表接口返回时逐家渲染店铺卡片', async () => {
    listShops.mockResolvedValue(SHOPS)
    const { wrapper } = await mountHome()
    expect(listShops).toHaveBeenCalledTimes(1)
    expect(wrapper.get('[data-testid="home-shop-1"]').text()).toContain('北洋餐厅')
    expect(wrapper.get('[data-testid="home-shop-1"]').text()).toContain('营业中')
    expect(wrapper.get('[data-testid="home-shop-2"]').text()).toContain('川渝小馆')
  })

  it('点击店铺卡片进入该店', async () => {
    listShops.mockResolvedValue(SHOPS)
    const { wrapper, router } = await mountHome()
    await wrapper.get('[data-testid="home-shop-1"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/shops/1')
  })

  it('列表接口异常时回退到示例店铺并提示', async () => {
    listShops.mockRejectedValue(new Error('offline'))
    const { wrapper } = await mountHome()
    const note = wrapper.get('[data-testid="home-feed-note"]')
    expect(note.text()).toContain('示例')
    // 示例店铺仍然可浏览
    expect(wrapper.get('[data-testid="home-shop-1"]').exists()).toBe(true)
  })
})
