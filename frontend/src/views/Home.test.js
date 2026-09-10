// @vitest-environment jsdom
// 首页店铺流（分页）测试。mock 掉 api 模块；页面须提供 data-testid：
// home-shop-<id> / home-feed-note / home-prev / home-next。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/shop', () => ({ listShops: vi.fn() }))

import { listShops } from '@/api/shop'
import { mountView } from '@/test/mountView'
import Home from './Home.vue'

const SHOP_A = { id: 1, shopName: '北洋餐厅', notice: '煎饼果子现做现卖', status: 'OPEN' }
const SHOP_B = { id: 2, shopName: '川渝小馆', notice: '麻辣鲜香', status: 'OPEN' }
const PAGE1 = { items: [SHOP_A, SHOP_B], page: 1, size: 20, total: 3, totalPages: 2 }
const PAGE2 = {
  items: [{ id: 3, shopName: '深夜烧烤', notice: '夜宵档', status: 'OPEN' }],
  page: 2,
  size: 20,
  total: 3,
  totalPages: 2,
}

async function mountHome() {
  const ctx = await mountView(Home, { path: '/' })
  await flushPromises()
  return ctx
}

describe('首页店铺流（分页）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('按分页参数取店铺并渲染', async () => {
    listShops.mockResolvedValue(PAGE1)
    const { wrapper } = await mountHome()
    expect(listShops).toHaveBeenCalledWith({ page: 1, size: 20 })
    expect(wrapper.get('[data-testid="home-shop-1"]').text()).toContain('北洋餐厅')
    expect(wrapper.get('[data-testid="home-shop-2"]').text()).toContain('川渝小馆')
  })

  it('点击店铺卡片进入该店', async () => {
    listShops.mockResolvedValue(PAGE1)
    const { wrapper, router } = await mountHome()
    await wrapper.get('[data-testid="home-shop-1"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/shops/1')
  })

  it('列表接口异常时提示加载失败且不伪造店铺', async () => {
    listShops.mockRejectedValue(new Error('offline'))
    const { wrapper } = await mountHome()
    const note = wrapper.get('[data-testid="home-feed-note"]')
    expect(note.text()).toContain('加载失败')
    expect(wrapper.find('[data-testid="home-shop-1"]').exists()).toBe(false)
  })
})
