// @vitest-environment jsdom
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/shop', () => ({ searchShops: vi.fn(), listShops: vi.fn() }))

import { searchShops, listShops } from '@/api/shop'
import { mountView } from '@/test/mountView'
import Search from './Search.vue'

const categoryShop = { id: 7, shopName: '北洋餐厅', notice: '快餐便当' }
const productShop = { id: 8, shopName: '梅园食堂', notice: '煎饼果子' }
const page = items => ({ items, page: 1, size: 20, total: items.length, totalPages: 1 })

async function mountSearch() {
  const ctx = await mountView(Search, { path: '/search' })
  await flushPromises()
  return ctx
}

async function submit(wrapper, keyword) {
  await wrapper.get('[data-testid="search-input"]').setValue(keyword)
  await wrapper.get('[data-testid="search-submit"]').trigger('click')
  await flushPromises()
}

describe('按经营类别和商品名称搜索店铺', () => {
  beforeEach(() => vi.clearAllMocks())

  it.each([
    ['快餐便当', categoryShop],
    ['煎饼果子', productShop],
  ])('输入“%s”时展示后端匹配的店铺', async (keyword, shop) => {
    searchShops.mockResolvedValue(page([shop]))
    const { wrapper } = await mountSearch()
    await submit(wrapper, `  ${keyword}  `)
    expect(searchShops).toHaveBeenCalledWith(keyword)
    expect(wrapper.get(`[data-testid="search-result-${shop.id}"]`).text()).toContain(shop.shopName)
    expect(wrapper.find('[data-testid="search-local-note"]').exists()).toBe(false)
  })

  it('空关键词不请求后端并给出提示', async () => {
    const { wrapper } = await mountSearch()
    await submit(wrapper, '   ')
    expect(searchShops).not.toHaveBeenCalled()
    expect(wrapper.get('[data-testid="search-message"]').text()).toContain('请输入')
  })

  it('无匹配结果时展示明确空状态', async () => {
    searchShops.mockResolvedValue(page([]))
    const { wrapper } = await mountSearch()
    await submit(wrapper, '不存在的商品')
    expect(wrapper.get('[data-testid="search-empty"]').text()).toContain('没有找到')
  })

  it('后端搜索失败时仅按店铺名降级并明确提示能力范围', async () => {
    searchShops.mockRejectedValue(new Error('server error'))
    listShops.mockResolvedValue(page([categoryShop]))
    const { wrapper } = await mountSearch()
    await submit(wrapper, '北洋')
    expect(wrapper.get('[data-testid="search-local-note"]').text()).toContain('仅按店铺名')
    expect(wrapper.get('[data-testid="search-result-7"]').text()).toContain('北洋餐厅')
  })

  it('连续搜索时后返回的旧请求不能覆盖新关键词结果', async () => {
    let resolveOld
    searchShops.mockImplementation(keyword => keyword === '快餐便当'
      ? new Promise(resolve => { resolveOld = resolve })
      : Promise.resolve(page([productShop])))
    const { wrapper } = await mountSearch()
    await wrapper.get('[data-testid="search-input"]').setValue('快餐便当')
    await wrapper.get('[data-testid="search-submit"]').trigger('click')
    await wrapper.get('[data-testid="search-input"]').setValue('煎饼果子')
    await wrapper.get('[data-testid="search-submit"]').trigger('click')
    await flushPromises()
    resolveOld(page([categoryShop]))
    await flushPromises()

    expect(wrapper.get('[data-testid="search-result-8"]').text()).toContain('梅园食堂')
    expect(wrapper.find('[data-testid="search-result-7"]').exists()).toBe(false)
  })
})
