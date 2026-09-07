// @vitest-environment jsdom
// 店铺页（顾客点单）红灯基线。mock 掉 api 模块；页面须提供 data-testid：
// shop-name / shop-notice / shop-status / category-<id> / product-card-<id> / add-<id> / shop-message。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/shop', () => ({
  getShop: vi.fn(),
  listCategories: vi.fn(),
  listProducts: vi.fn(),
}))
vi.mock('@/api/cart', () => ({ addToCart: vi.fn() }))

import { addToCart } from '@/api/cart'
import { getShop, listCategories, listProducts } from '@/api/shop'
import { mountView } from '@/test/mountView'
import Shop from './Shop.vue'

const SHOP = { id: 7, merchantId: 12, shopName: '北洋餐厅', notice: '欢迎光临', status: 'OPEN' }
const CATEGORIES = [
  { id: 30, shopId: 7, name: '热销', sort: 1 },
  { id: 31, shopId: 7, name: '饮品', sort: 2 },
]
const PRODUCTS = {
  30: [{ id: 40, categoryId: 30, name: '煎饼果子', description: '现做现卖', price: 8.5, stock: 20, status: 'ON_SALE' }],
  31: [{ id: 44, categoryId: 31, name: '柠檬茶', description: '现泡', price: 6, stock: 10, status: 'ON_SALE' }],
}

async function mountShop(shop = SHOP) {
  getShop.mockResolvedValue(shop)
  listCategories.mockResolvedValue(CATEGORIES)
  listProducts.mockImplementation(async (_shopId, categoryId) => PRODUCTS[categoryId] || [])
  const ctx = await mountView(Shop, { path: '/shops/7' })
  await flushPromises()
  return ctx
}

describe('店铺页（顾客点单）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('展示店铺名称、公告与营业状态', async () => {
    const { wrapper } = await mountShop()
    expect(wrapper.get('[data-testid="shop-name"]').text()).toContain('北洋餐厅')
    expect(wrapper.get('[data-testid="shop-notice"]').text()).toContain('欢迎光临')
    expect(wrapper.get('[data-testid="shop-status"]').text()).toContain('营业中')
  })

  it('默认选中第一个分类并展示其商品', async () => {
    const { wrapper } = await mountShop()
    expect(listProducts).toHaveBeenCalledWith(7, 30)
    const card = wrapper.get('[data-testid="product-card-40"]')
    expect(card.text()).toContain('煎饼果子')
    expect(card.text()).toContain('8.50')
    expect(wrapper.text()).not.toContain('柠檬茶')
  })

  it('点击其他分类切换展示对应商品', async () => {
    const { wrapper } = await mountShop()
    await wrapper.get('[data-testid="category-31"]').trigger('click')
    await flushPromises()
    expect(listProducts).toHaveBeenCalledWith(7, 31)
    expect(wrapper.get('[data-testid="product-card-44"]').text()).toContain('柠檬茶')
    expect(wrapper.text()).not.toContain('煎饼果子')
  })

  it('点击加购调用 addToCart 并提示成功', async () => {
    addToCart.mockResolvedValue({ id: 50, productId: 40, quantity: 1 })
    const { wrapper } = await mountShop()
    await wrapper.get('[data-testid="add-40"]').trigger('click')
    await flushPromises()
    expect(addToCart).toHaveBeenCalledWith({ productId: 40, quantity: 1 })
    expect(wrapper.get('[data-testid="shop-message"]').text()).toContain('已加入购物车')
  })

  it('加购失败（库存不足/下架）展示后端提示', async () => {
    addToCart.mockRejectedValue(new Error('库存不足'))
    const { wrapper } = await mountShop()
    await wrapper.get('[data-testid="add-40"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="shop-message"]').text()).toContain('库存不足')
  })

  it('店铺非营业状态时禁止加购', async () => {
    const { wrapper } = await mountShop({ ...SHOP, status: 'CLOSED' })

    expect(wrapper.get('[data-testid="add-40"]').element.disabled).toBe(true)
    expect(addToCart).not.toHaveBeenCalled()
  })
})
