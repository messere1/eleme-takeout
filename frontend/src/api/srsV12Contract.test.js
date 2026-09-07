// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

import { http } from './http'
import * as cartApi from './cart'
import * as orderApi from './order'
import * as shopApi from './shop'

describe('SRS V1.2 前端接口契约', () => {
  beforeEach(() => vi.clearAllMocks())

  it('店铺列表使用公开分页接口', async () => {
    await shopApi.listShops({ page: 1, size: 20 })
    expect(http.get).toHaveBeenCalledWith('/shops', { params: { page: 1, size: 20 } })
  })

  it('商品列表按分类分页查询', async () => {
    await shopApi.listProducts(30, { page: 1, size: 20 })
    expect(http.get).toHaveBeenCalledWith('/categories/30/products', {
      params: { page: 1, size: 20 },
    })
  })

  it('提供公开商品详情接口', () => {
    expect(shopApi.getProduct).toBeTypeOf('function')
  })

  it('提供独立的商品价格修改接口', () => {
    expect(shopApi.updateProductPrice).toBeTypeOf('function')
  })

  it('购物车新增和修改使用SRS约定路径与方法', async () => {
    await cartApi.addToCart({ productId: 40, quantity: 1 })
    expect(http.post).toHaveBeenCalledWith('/cart/items', { productId: 40, quantity: 1 })

    await cartApi.updateItem(50, 2)
    expect(http.patch).toHaveBeenCalledWith('/cart/items/50', { quantity: 2 })
    expect(http.put).not.toHaveBeenCalled()
  })

  it('订单查询使用CREATED状态和默认20条分页', async () => {
    await orderApi.listOrders({ page: 1, size: 20, status: 'CREATED' })
    expect(http.get).toHaveBeenCalledWith('/orders', {
      params: { page: 1, size: 20, status: 'CREATED' },
    })
  })
})
