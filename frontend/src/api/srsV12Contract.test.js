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
import * as userApi from './user'
import * as merchantApi from './merchant'
import * as riderApi from './rider'

describe('SRS V1.4 前端接口契约（文件名保留以兼容历史链接）', () => {
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

  it('商家通过店铺营业时间接口保存开始和结束时间', async () => {
    await shopApi.updateBusinessHours(7, '08:00', '21:00')
    expect(http.patch).toHaveBeenCalledWith('/shops/7/business-hours', {
      openingTime: '08:00',
      closingTime: '21:00',
    })
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

  it('下单时完整传递收货信息快照', async () => {
    const recipient = {
      recipientName: '张同学',
      recipientPhone: '02285356000',
      deliveryAddress: '天津大学北洋园校区学生宿舍1号楼',
    }

    await orderApi.createOrder(recipient)

    expect(http.post).toHaveBeenCalledWith('/orders', recipient)
  })

  // SRS V2 移除了商家直接完成的 POST /orders/{id}/complete，完成只剩「骑手送达 → 顾客确认收货」一条路径。
  it('订单流转使用SRS V2约定的接单与确认收货接口', async () => {
    await orderApi.acceptOrder(60)
    await orderApi.confirmOrder(60)

    expect(http.post).toHaveBeenNthCalledWith(1, '/orders/60/accept')
    expect(http.post).toHaveBeenNthCalledWith(2, '/orders/60/confirm')
  })

  it('管理员账号清单使用只读接口和默认20条分页', async () => {
    await userApi.listUsers()
    await merchantApi.listMerchants()

    expect(http.get).toHaveBeenNthCalledWith(1, '/admin/users', {
      params: { page: 1, size: 20 },
    })
    expect(http.get).toHaveBeenNthCalledWith(2, '/admin/merchants', {
      params: { page: 1, size: 20 },
    })
  })

  it('骑手注册使用公开骑手资源接口', async () => {
    const payload = { riderName: '骑手小王', phone: '13700137000', password: 'abc12345' }
    await riderApi.registerRider(payload)
    expect(http.post).toHaveBeenCalledWith('/riders', payload)
  })
})
