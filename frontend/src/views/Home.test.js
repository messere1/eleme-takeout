// @vitest-environment jsdom
// 首页店铺流（分页）测试。mock 掉 api 模块；页面须提供 data-testid：
// home-shop-<id> / home-feed-note / home-prev / home-next。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/shop', () => ({
  listShops: vi.fn(),
  listRecommendedShops: vi.fn().mockResolvedValue([]),
}))
vi.mock('@/api/merchant', () => ({
  listBusinessCategories: vi.fn().mockResolvedValue([
    { id: 2, name: '奶茶饮品', enabled: true },
  ]),
}))

import { listRecommendedShops, listShops } from '@/api/shop'
import { listBusinessCategories } from '@/api/merchant'
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

function touchEvent(type, clientX, clientY) {
  const event = new Event(type, { cancelable: true })
  event.touches = [{ clientX, clientY }]
  return event
}

function makeScrollable(block) {
  Object.defineProperty(block, 'clientHeight', { value: 200, configurable: true })
  Object.defineProperty(block, 'scrollHeight', { value: 600, configurable: true })
  block.scrollTop = 0
}

describe('首页店铺流（分页）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listBusinessCategories.mockResolvedValue([{ id: 2, name: '奶茶饮品', enabled: true }])
    listShops.mockResolvedValue(PAGE1)
    listRecommendedShops.mockResolvedValue([])
  })

  it('有推荐结果时展示猜你喜欢，请求条数固定为 10', async () => {
    listRecommendedShops.mockResolvedValue([SHOP_B])

    const ctx = await mountHome()

    expect(ctx.wrapper.find('[data-testid="home-recommend"]').exists()).toBe(true)
    expect(ctx.wrapper.find('[data-testid="home-recommend-2"]').exists()).toBe(true)
    expect(listRecommendedShops).toHaveBeenCalledWith(10)
  })

  it('推荐位复用频道的横向滚轮逻辑，滚到自己两端就把滚动交回页面', async () => {
    listRecommendedShops.mockResolvedValue([SHOP_A, SHOP_B])

    const ctx = await mountHome()
    const row = ctx.wrapper.find('.recommend-row')

    const event = new Event('wheel', { cancelable: true })
    event.deltaY = 100
    row.element.dispatchEvent(event)

    expect(event.defaultPrevented).toBe(false)
  })

  it('触摸拖动也能滚动店铺列表，并拦住整页跟着滚', async () => {
    const ctx = await mountHome()
    const block = ctx.wrapper.get('.browse-block').element
    makeScrollable(block)

    window.dispatchEvent(touchEvent('touchstart', 0, 300))
    const move = touchEvent('touchmove', 0, 200)
    window.dispatchEvent(move)

    expect(block.scrollTop).toBe(100)
    expect(move.defaultPrevented).toBe(true)
  })

  it('横向为主的滑动不交给纵向接力，留给品类条原生横滑', async () => {
    const ctx = await mountHome()
    const block = ctx.wrapper.get('.browse-block').element
    makeScrollable(block)

    window.dispatchEvent(touchEvent('touchstart', 300, 100))
    const move = touchEvent('touchmove', 100, 100)
    window.dispatchEvent(move)

    expect(block.scrollTop).toBe(0)
    expect(move.defaultPrevented).toBe(false)
  })

  it('推荐位有店铺照片时显示图片', async () => {
    listRecommendedShops.mockResolvedValue([{ ...SHOP_B, imageUrl: '/uploads/shop-b.png' }])

    const ctx = await mountHome()

    expect(ctx.wrapper.get('[data-testid="home-recommend-image-2"]').attributes('src'))
      .toBe('/uploads/shop-b.png')
  })

  it('推荐位没有照片时退回 emoji 占位块', async () => {
    listRecommendedShops.mockResolvedValue([SHOP_B])

    const ctx = await mountHome()
    const card = ctx.wrapper.get('[data-testid="home-recommend-2"]')

    expect(card.find('[data-testid="home-recommend-image-2"]').exists()).toBe(false)
    expect(card.find('img').exists()).toBe(false)
    expect(card.text()).toContain('川渝小馆')
  })

  it('推荐为空时不展示猜你喜欢整块', async () => {
    const ctx = await mountHome()

    expect(ctx.wrapper.find('[data-testid="home-recommend"]').exists()).toBe(false)
  })

  it('推荐接口失败不影响首页店铺流', async () => {
    listRecommendedShops.mockRejectedValue(new Error('offline'))

    const ctx = await mountHome()

    expect(ctx.wrapper.find('[data-testid="home-recommend"]').exists()).toBe(false)
    expect(ctx.wrapper.find('[data-testid="home-shop-1"]').exists()).toBe(true)
  })

  it('商家传过店铺照片时卡片显示图片', async () => {
    listShops.mockResolvedValue({
      ...PAGE1,
      items: [{ ...SHOP_A, imageUrl: '/uploads/shop-a.png' }],
    })
    const { wrapper } = await mountHome()

    expect(wrapper.get('[data-testid="home-shop-image-1"]').attributes('src'))
      .toBe('/uploads/shop-a.png')
  })

  it('没有店铺照片时退回 emoji 占位块', async () => {
    listShops.mockResolvedValue(PAGE1)
    const { wrapper } = await mountHome()

    expect(wrapper.find('[data-testid="home-shop-image-1"]').exists()).toBe(false)
    expect(wrapper.get('[data-testid="home-shop-1"]').find('img').exists()).toBe(false)
    expect(wrapper.get('[data-testid="home-shop-1"]').text()).toContain('北洋餐厅')
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

  it('点击经营品类后按品类ID重新查询店铺', async () => {
    listShops.mockResolvedValue(PAGE1)
    const { wrapper } = await mountHome()
    await wrapper.get('[data-testid="home-category-2"]').trigger('click')
    await flushPromises()
    expect(listShops).toHaveBeenLastCalledWith({ page: 1, size: 20, businessCategoryId: 2 })
  })

  it('快速切换经营品类时只显示最后选中的店铺列表', async () => {
    let resolveFirstCategory
    listBusinessCategories.mockResolvedValue([
      { id: 2, name: '奶茶饮品', enabled: true },
      { id: 3, name: '日韩料理', enabled: true },
    ])
    listShops.mockImplementation((params) => {
      if (params.businessCategoryId === 2) {
        return new Promise(resolve => { resolveFirstCategory = resolve })
      }
      if (params.businessCategoryId === 3) return Promise.resolve(PAGE2)
      return Promise.resolve(PAGE1)
    })

    const { wrapper } = await mountHome()
    await wrapper.get('[data-testid="home-category-2"]').trigger('click')
    await wrapper.get('[data-testid="home-category-3"]').trigger('click')
    resolveFirstCategory(PAGE1)
    await flushPromises()

    expect(listShops).toHaveBeenCalledWith({ page: 1, size: 20, businessCategoryId: 3 })
    expect(wrapper.get('[data-testid="home-shop-3"]').text()).toContain('深夜烧烤')
    expect(wrapper.find('[data-testid="home-shop-1"]').exists()).toBe(false)
  })
})
