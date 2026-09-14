// @vitest-environment jsdom
// 商家信息页的上传区。此前用的是原生 <input type="file">（无样式、窄屏挤成两行、
// 上传后没有预览），这里锁住与「个人资料」「店铺管理」一致的交互：
// 预览图 + 隐藏的原生 input + 样式化按钮，并且真的调用上传接口。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/merchant', () => ({
  getMyMerchant: vi.fn(),
  updateMyMerchant: vi.fn(),
  listBusinessCategories: vi.fn(),
  deleteMyMerchant: vi.fn(),
  getMyShopBusinessCategories: vi.fn(),
  updateMyShopBusinessCategories: vi.fn(),
}))
vi.mock('@/api/upload', () => ({ uploadImage: vi.fn() }))

import { getMyMerchant, getMyShopBusinessCategories, listBusinessCategories } from '@/api/merchant'
import { uploadImage } from '@/api/upload'
import { mountView } from '@/test/mountView'
import MerchantProfile from './MerchantProfile.vue'

const ME = {
  merchantName: 'MCDo',
  phone: '15893887356',
  businessScope: '西式简餐',
  shopName: 'MCDo',
  shopStatus: 'OPEN',
  shopAddress: '天津大学急急急',
  shopId: 15,
  imageUrl: null,
  coverImageUrl: null,
}

async function mountProfile(me = ME) {
  getMyMerchant.mockResolvedValue({ ...me })
  listBusinessCategories.mockResolvedValue([])
  getMyShopBusinessCategories.mockResolvedValue([])
  const ctx = await mountView(MerchantProfile, { path: '/merchant/profile' })
  await flushPromises()
  return ctx
}

function pickFile(input, name = 'pic.png') {
  Object.defineProperty(input.element, 'files', {
    value: [new File(['x'], name, { type: 'image/png' })],
  })
}

describe('商家信息页上传区', () => {
  beforeEach(() => vi.clearAllMocks())

  it('两个上传入口都用隐藏的原生 input + 样式化按钮，不再内联原生控件', async () => {
    const { wrapper } = await mountProfile()

    const shopInput = wrapper.get('[data-testid="profile-shop-image-input"]')
    const coverInput = wrapper.get('[data-testid="profile-cover-image-input"]')

    expect(shopInput.attributes('type')).toBe('file')
    expect(coverInput.attributes('type')).toBe('file')
    // 原生控件靠 file-input 隐藏，用户看到的是样式化按钮文案
    expect(shopInput.classes()).toContain('file-input')
    expect(coverInput.classes()).toContain('file-input')
    expect(wrapper.findAll('.upload-btn')).toHaveLength(2)
    expect(wrapper.text()).toContain('上传照片')
    expect(wrapper.text()).toContain('上传封面')
    // 没有图片时展示占位块，而不是空白
    expect(wrapper.findAll('.upload-thumb.placeholder')).toHaveLength(2)
  })

  it('已保存的店铺照片和封面会作为预览回填', async () => {
    const { wrapper } = await mountProfile({
      ...ME, imageUrl: '/uploads/shop.png', coverImageUrl: '/uploads/cover.png',
    })

    expect(wrapper.get('.upload-thumb.square').attributes('src')).toBe('/uploads/shop.png')
    expect(wrapper.get('.upload-thumb.wide').attributes('src')).toBe('/uploads/cover.png')
  })

  it('上传店铺照片调用接口并携带本店 shopId', async () => {
    uploadImage.mockResolvedValue({ url: '/uploads/new-shop.png' })
    const { wrapper } = await mountProfile()

    const input = wrapper.get('[data-testid="profile-shop-image-input"]')
    pickFile(input)
    await input.trigger('change')
    await flushPromises()

    expect(uploadImage).toHaveBeenCalledWith(expect.any(File), 'SHOP_IMAGE', 15)
    expect(wrapper.get('.upload-thumb.square').attributes('src')).toBe('/uploads/new-shop.png')
    expect(wrapper.text()).toContain('图片已上传')
  })

  it('上传封面调用接口并用 SHOP_COVER 目标', async () => {
    uploadImage.mockResolvedValue({ url: '/uploads/new-cover.png' })
    const { wrapper } = await mountProfile()

    const input = wrapper.get('[data-testid="profile-cover-image-input"]')
    pickFile(input)
    await input.trigger('change')
    await flushPromises()

    expect(uploadImage).toHaveBeenCalledWith(expect.any(File), 'SHOP_COVER', 15)
    expect(wrapper.get('.upload-thumb.wide').attributes('src')).toBe('/uploads/new-cover.png')
  })

  it('上传失败时展示后端原因且不改变预览', async () => {
    uploadImage.mockRejectedValue(new Error('仅支持不超过5MiB的JPEG、PNG或WebP图片'))
    const { wrapper } = await mountProfile()

    const input = wrapper.get('[data-testid="profile-shop-image-input"]')
    pickFile(input)
    await input.trigger('change')
    await flushPromises()

    expect(wrapper.text()).toContain('JPEG')
    expect(wrapper.find('.upload-thumb.placeholder').exists()).toBe(true)
  })

  it('还没有店铺时不发上传请求，提示无法上传', async () => {
    const { wrapper } = await mountProfile({ ...ME, shopId: null })

    const input = wrapper.get('[data-testid="profile-shop-image-input"]')
    pickFile(input)
    await input.trigger('change')
    await flushPromises()

    expect(uploadImage).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('尚未确定上传目标')
  })
})
