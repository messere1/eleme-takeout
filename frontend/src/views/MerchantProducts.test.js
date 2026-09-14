// @vitest-environment jsdom
// 商家商品管理页红灯基线。mock 掉 api 模块；页面须提供 data-testid：
// product-mgmt-item-<id> / product-mgmt-status-<id> / product-mgmt-stock-<id> /
// product-mgmt-save-stock-<id> / product-mgmt-delete-<id> /
// product-category / product-name / product-price / product-stock / product-create-btn / product-message。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/shop', () => ({
  getMyShop: vi.fn(),
  listMerchantProducts: vi.fn(),
  listCategories: vi.fn(),
  createProduct: vi.fn(),
  changeProductStatus: vi.fn(),
  updateProductStock: vi.fn(),
  updateProduct: vi.fn(),
  updateProductPrice: vi.fn(),
  deleteProduct: vi.fn(),
}))

import { changeProductStatus, createProduct, deleteProduct, getMyShop, listCategories, listMerchantProducts, updateProductPrice, updateProductStock } from '@/api/shop'
import { session } from '@/utils/session'
import { mountView } from '@/test/mountView'
import MerchantProducts from './MerchantProducts.vue'

const PRODUCTS = [
  { id: 40, categoryId: 30, name: '煎饼果子', description: '现做现卖', price: 8.5, stock: 20, status: 'ON_SALE' },
  { id: 44, categoryId: 31, name: '柠檬茶', description: '现泡', price: 6, stock: 10, status: 'OFF_SALE' },
]
const CATEGORIES = [
  { id: 30, shopId: 7, name: '热销', sort: 1 },
  { id: 31, shopId: 7, name: '饮品', sort: 2 },
]

async function mountProducts() {
  session.save({ token: 'mt-1', role: 'MERCHANT' })
  session.saveShop({ merchantId: 12, shopId: 7, shopName: '北洋餐厅' })
  getMyShop.mockResolvedValue({ id: 7, shopName: '北洋餐厅' })
  listMerchantProducts.mockResolvedValue(PRODUCTS)
  listCategories.mockResolvedValue(CATEGORIES)
  const ctx = await mountView(MerchantProducts, { path: '/merchant/products' })
  await flushPromises()
  return ctx
}

async function setField(wrapper, testid, value) {
  const control = wrapper.get(`[data-testid="${testid}"]`)
  const input = control.element.tagName === 'INPUT' ? control : control.find('input')
  await input.setValue(value)
}

describe('商家商品管理', () => {
  beforeEach(() => {
    session.clear()
    vi.clearAllMocks()
  })

  // 登录接口不返回 shopId，本地缓存只在注册时写过，退出登录/401 会清掉它。
  // 商家退出再登录后不能因此进不了商品管理。
  it('本地没有店铺缓存时按登录身份找回店铺并加载商品', async () => {
    session.save({ token: 'mt-1', role: 'MERCHANT' }) // 有登录态，但没有 takeout-shop
    getMyShop.mockResolvedValue({ id: 7, shopName: '北洋餐厅' })
    listMerchantProducts.mockResolvedValue(PRODUCTS)
    listCategories.mockResolvedValue(CATEGORIES)

    const { wrapper } = await mountView(MerchantProducts, { path: '/merchant/products' })
    await flushPromises()

    expect(wrapper.find('.console-missing').exists()).toBe(false)
    expect(wrapper.get('[data-testid="product-mgmt-item-40"]').exists()).toBe(true)
  })

  it('缓存和接口都拿不到店铺时才提示去注册', async () => {
    session.save({ token: 'mt-1', role: 'MERCHANT' })
    getMyShop.mockRejectedValue(new Error('未绑定店铺'))

    const { wrapper } = await mountView(MerchantProducts, { path: '/merchant/products' })
    await flushPromises()

    expect(wrapper.get('.console-missing').text()).toContain('还没有店铺')
  })

  // FR-010 / EX-029：价格不接受指数形式、三位小数，也不得先舍入再接受。
  it('改价时拒绝三位小数与指数形式，不发请求', async () => {
    const { wrapper } = await mountProducts()

    for (const bad of ['1.234', '1e-7', '0', '-1']) {
      await wrapper.get('[data-testid="product-mgmt-price-40"]').setValue(bad)
      await wrapper.get('[data-testid="product-mgmt-save-price-40"]').trigger('click')
      await flushPromises()
      expect(updateProductPrice).not.toHaveBeenCalled()
      expect(wrapper.text()).toContain('0.01～99999999.99')
    }
  })

  it('改价接受两位小数以内金额', async () => {
    updateProductPrice.mockResolvedValue({ id: 40, price: 9.5 })
    const { wrapper } = await mountProducts()

    await wrapper.get('[data-testid="product-mgmt-price-40"]').setValue('9.50')
    await wrapper.get('[data-testid="product-mgmt-save-price-40"]').trigger('click')
    await flushPromises()

    expect(updateProductPrice).toHaveBeenCalledWith(40, 9.5)
  })

  it('列出在售与下架的全部商品', async () => {
    const { wrapper } = await mountProducts()
    expect(listMerchantProducts).toHaveBeenCalledTimes(1)
    const onSale = wrapper.get('[data-testid="product-mgmt-item-40"]')
    expect(onSale.text()).toContain('煎饼果子')
    expect(onSale.text()).toContain('在售')
    expect(wrapper.get('[data-testid="product-mgmt-item-44"]').text()).toContain('已下架')
  })

  it('切换商品上下架调用 changeProductStatus 并更新状态', async () => {
    changeProductStatus.mockResolvedValue({ ...PRODUCTS[0], status: 'OFF_SALE' })
    const { wrapper } = await mountProducts()
    await wrapper.get('[data-testid="product-mgmt-status-40"]').trigger('click')
    await flushPromises()
    expect(changeProductStatus).toHaveBeenCalledWith(40, 'OFF_SALE')
    expect(wrapper.get('[data-testid="product-mgmt-item-40"]').text()).toContain('已下架')
  })

  it('修改库存调用 updateProductStock', async () => {
    updateProductStock.mockResolvedValue({ ...PRODUCTS[0], stock: 30 })
    const { wrapper } = await mountProducts()
    await setField(wrapper, 'product-mgmt-stock-40', '30')
    await wrapper.get('[data-testid="product-mgmt-save-stock-40"]').trigger('click')
    await flushPromises()
    expect(updateProductStock).toHaveBeenCalledWith(40, 30)
    expect(wrapper.get('[data-testid="product-mgmt-item-40"]').text()).toContain('30')
  })

  it('删除商品调用 deleteProduct 并移除该行', async () => {
    deleteProduct.mockResolvedValue(null)
    const { wrapper } = await mountProducts()
    await wrapper.get('[data-testid="product-mgmt-delete-44"]').trigger('click')
    await flushPromises()
    expect(deleteProduct).toHaveBeenCalledWith(44)
    expect(wrapper.find('[data-testid="product-mgmt-item-44"]').exists()).toBe(false)
  })

  it('新增商品调用 createProduct 并追加展示', async () => {
    createProduct.mockResolvedValue({ id: 45, categoryId: 30, name: '豆浆', price: 3.5, stock: 10, status: 'OFF_SALE' })
    const { wrapper } = await mountProducts()
    await wrapper.get('[data-testid="product-category"]').setValue('30')
    await setField(wrapper, 'product-name', '豆浆')
    await setField(wrapper, 'product-price', '3.5')
    await setField(wrapper, 'product-stock', '10')
    await wrapper.get('[data-testid="product-create-btn"]').trigger('click')
    await flushPromises()
    expect(createProduct).toHaveBeenCalledWith(7, {
      categoryId: 30,
      name: '豆浆',
      description: '',
      price: 3.5,
      stock: 10,
    })
    expect(wrapper.get('[data-testid="product-mgmt-item-45"]').text()).toContain('豆浆')
  })

  it('商品初始加载失败时展示用户可读错误状态', async () => {
    session.save({ token: 'mt-1', role: 'MERCHANT' })
    session.saveShop({ merchantId: 12, shopId: 7, shopName: '北洋餐厅' })
    listCategories.mockRejectedValue(new Error('服务器异常'))
    listMerchantProducts.mockResolvedValue([])

    const { wrapper } = await mountView(MerchantProducts, { path: '/merchant/products' })
    await flushPromises()

    expect(wrapper.get('[data-testid="product-message"]').text()).toContain('服务器异常')
  })

  it('创建商品请求未完成时禁用创建按钮', async () => {
    createProduct.mockReturnValue(new Promise(() => {}))
    const { wrapper } = await mountProducts()
    await wrapper.get('[data-testid="product-category"]').setValue('30')
    await setField(wrapper, 'product-name', '豆浆')
    await setField(wrapper, 'product-price', '3.5')
    await setField(wrapper, 'product-stock', '10')

    await wrapper.get('[data-testid="product-create-btn"]').trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.get('[data-testid="product-create-btn"]').element.disabled).toBe(true)
    expect(createProduct).toHaveBeenCalledTimes(1)
  })
})
