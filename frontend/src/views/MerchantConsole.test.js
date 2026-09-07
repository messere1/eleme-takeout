// @vitest-environment jsdom
// console-shop-name / console-shop-status / console-status / console-name-input /
// console-notice-input / console-save-shop / console-message /
// category-item-<id> / category-delete-<id> / category-name-input / category-sort-input / category-create-btn。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/shop', () => ({
  getShop: vi.fn(),
  updateShop: vi.fn(),
  changeStatus: vi.fn(),
  listCategories: vi.fn(),
  createCategory: vi.fn(),
  deleteCategory: vi.fn(),
}))

import { changeStatus, createCategory, deleteCategory, getShop, listCategories, updateShop } from '@/api/shop'
import { session } from '@/utils/session'
import { mountView } from '@/test/mountView'
import MerchantConsole from './MerchantConsole.vue'

const SHOP = { id: 7, merchantId: 12, shopName: '北洋餐厅', notice: '欢迎光临', status: 'OPEN' }
const CATEGORIES = [{ id: 30, shopId: 7, name: '热销', sort: 1 }]

async function mountConsole() {
  session.save({ token: 'mt-1', role: 'MERCHANT' })
  session.saveShop({ merchantId: 12, shopId: 7, shopName: '北洋餐厅' })
  getShop.mockResolvedValue(SHOP)
  listCategories.mockResolvedValue(CATEGORIES)
  const ctx = await mountView(MerchantConsole, { path: '/merchant' })
  await flushPromises()
  return ctx
}

async function setField(wrapper, testid, value) {
  const control = wrapper.get(`[data-testid="${testid}"]`)
  const input = control.element.tagName === 'INPUT' ? control : control.find('input')
  await input.setValue(value)
}

describe('商家后台', () => {
  beforeEach(() => {
    session.clear()
    vi.clearAllMocks()
  })

  it('展示店铺名称、营业状态与分类列表', async () => {
    const { wrapper } = await mountConsole()
    expect(getShop).toHaveBeenCalledWith(7)
    expect(wrapper.get('[data-testid="console-shop-name"]').text()).toContain('北洋餐厅')
    expect(wrapper.get('[data-testid="console-shop-status"]').text()).toContain('营业中')
    expect(wrapper.get('[data-testid="category-item-30"]').text()).toContain('热销')
  })

  it('切换营业状态调用 changeStatus 并更新显示', async () => {
    changeStatus.mockResolvedValue({ ...SHOP, status: 'CLOSED' })
    const { wrapper } = await mountConsole()
    await wrapper.get('[data-testid="console-status"]').setValue('CLOSED')
    await flushPromises()
    expect(changeStatus).toHaveBeenCalledWith(7, 'CLOSED')
    expect(wrapper.get('[data-testid="console-shop-status"]').text()).toContain('休息中')
  })

  it('保存店铺资料调用 updateShop 并提示已保存', async () => {
    updateShop.mockResolvedValue({ ...SHOP, shopName: '北洋餐厅二分店' })
    const { wrapper } = await mountConsole()
    await setField(wrapper, 'console-name-input', '北洋餐厅二分店')
    await setField(wrapper, 'console-notice-input', '营业 08:00-21:00')
    await wrapper.get('[data-testid="console-save-shop"]').trigger('click')
    await flushPromises()
    expect(updateShop).toHaveBeenCalledWith(7, {
      shopName: '北洋餐厅二分店',
      notice: '营业 08:00-21:00',
    })
    expect(wrapper.get('[data-testid="console-message"]').text()).toContain('已保存')
  })

  it('新增分类调用 createCategory 并追加展示', async () => {
    createCategory.mockResolvedValue({ id: 31, shopId: 7, name: '饮品', sort: 2 })
    const { wrapper } = await mountConsole()
    await setField(wrapper, 'category-name-input', '饮品')
    await setField(wrapper, 'category-sort-input', '2')
    await wrapper.get('[data-testid="category-create-btn"]').trigger('click')
    await flushPromises()
    expect(createCategory).toHaveBeenCalledWith(7, { name: '饮品', sort: 2 })
    expect(wrapper.text()).toContain('饮品')
  })

  it('删除空分类调用 deleteCategory 并移除该行', async () => {
    deleteCategory.mockResolvedValue(null)
    const { wrapper } = await mountConsole()
    await wrapper.get('[data-testid="category-delete-30"]').trigger('click')
    await flushPromises()
    expect(deleteCategory).toHaveBeenCalledWith(30)
    expect(wrapper.find('[data-testid="category-item-30"]').exists()).toBe(false)
  })
})
