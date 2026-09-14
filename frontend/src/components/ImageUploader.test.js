// @vitest-environment jsdom
import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/upload', () => ({ uploadImage: vi.fn() }))

import { uploadImage } from '@/api/upload'
import ImageUploader from './ImageUploader.vue'

function fixture(targetType = 'USER_AVATAR', targetId = 7, modelValue = '') {
  return mount(ImageUploader, { props: { targetType, targetId, modelValue, inputTestid: 'image-input' } })
}

async function pick(wrapper, file = new File(['png'], 'picture.png', { type: 'image/png' })) {
  const input = wrapper.get('[data-testid="image-input"]')
  Object.defineProperty(input.element, 'files', { configurable: true, value: [file] })
  await input.trigger('change')
  return input
}

describe('头像、店铺和商品图片上传', () => {
  beforeEach(() => vi.clearAllMocks())

  it.each([
    ['USER_AVATAR', 7],
    ['SHOP_IMAGE', 20],
    ['SHOP_COVER', 20],
    ['PRODUCT_IMAGE', 40],
  ])('%s 携带正确目标并回填返回的图片地址', async (targetType, targetId) => {
    uploadImage.mockResolvedValue({ url: '/uploads/saved.png' })
    const wrapper = fixture(targetType, targetId)
    const file = new File(['png'], 'picture.png', { type: 'image/png' })
    await pick(wrapper, file)
    await flushPromises()
    expect(uploadImage).toHaveBeenCalledWith(file, targetType, targetId)
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['/uploads/saved.png'])
  })

  it('上传期间禁用文件选择，失败后保留旧图且可重试', async () => {
    let rejectUpload
    uploadImage.mockImplementationOnce(() => new Promise((_, reject) => { rejectUpload = reject }))
    const wrapper = fixture('USER_AVATAR', 7, '/uploads/old.png')
    const input = await pick(wrapper)
    expect(input.element.disabled).toBe(true)
    rejectUpload(new Error('图片过大'))
    await flushPromises()
    expect(input.element.disabled).toBe(false)
    expect(wrapper.get('[data-testid="image-input-preview"]').attributes('src')).toBe('/uploads/old.png')
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
    expect(wrapper.emitted('message')?.at(-1)).toEqual(['图片过大'])

    uploadImage.mockResolvedValueOnce({ url: '/uploads/new.png' })
    await pick(wrapper)
    await flushPromises()
    expect(uploadImage).toHaveBeenCalledTimes(2)
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['/uploads/new.png'])
  })

  it('目标资源未确定时不上传，明确提示', async () => {
    const wrapper = fixture('PRODUCT_IMAGE', null)
    await pick(wrapper)
    expect(uploadImage).not.toHaveBeenCalled()
    expect(wrapper.emitted('message')?.[0]).toEqual(['尚未确定上传目标，请稍后重试'])
  })

  it('文件选择仅声明 JPEG、PNG 和 WebP', () => {
    const wrapper = fixture()
    expect(wrapper.get('[data-testid="image-input"]').attributes('accept')).toBe('image/jpeg,image/png,image/webp')
  })
})
