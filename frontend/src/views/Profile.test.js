// @vitest-environment jsdom
// 个人资料页（FR-003）红灯基线。mock 掉 api 模块；页面须提供 data-testid：
// profile-nickname / profile-phone / profile-address / profile-save / profile-message。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/user', () => ({ getProfile: vi.fn(), updateProfile: vi.fn() }))

import { getProfile, updateProfile } from '@/api/user'
import { mountView } from '@/test/mountView'
import Profile from './Profile.vue'

const ME = {
  id: 1,
  username: 'beiyang_user',
  phone: '13800138000',
  nickname: '北洋用户',
  address: '天津市津南区海河教育园',
}

async function mountProfile() {
  getProfile.mockResolvedValue(ME)
  const ctx = await mountView(Profile, { path: '/profile' })
  await flushPromises()
  return ctx
}

async function setField(wrapper, testid, value) {
  const control = wrapper.get(`[data-testid="${testid}"]`)
  const input = control.element.tagName === 'INPUT' ? control : control.find('input')
  await input.setValue(value)
}

describe('个人资料页', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('加载后回填昵称、手机号与地址', async () => {
    const { wrapper } = await mountProfile()
    expect(getProfile).toHaveBeenCalled()
    expect(wrapper.get('[data-testid="profile-nickname"]').element.value).toBe('北洋用户')
    expect(wrapper.get('[data-testid="profile-phone"]').element.value).toBe('13800138000')
    expect(wrapper.get('[data-testid="profile-address"]').element.value).toBe('天津市津南区海河教育园')
  })

  it('修改资料并保存时调用 updateProfile 并提示成功', async () => {
    updateProfile.mockResolvedValue({ ...ME, nickname: '新昵称' })
    const { wrapper } = await mountProfile()
    await setField(wrapper, 'profile-nickname', '新昵称')
    await wrapper.get('[data-testid="profile-save"]').trigger('click')
    await flushPromises()
    expect(updateProfile).toHaveBeenCalledWith({
      nickname: '新昵称',
      phone: '13800138000',
      address: '天津市津南区海河教育园',
    })
    expect(wrapper.get('[data-testid="profile-message"]').text()).toContain('已保存')
  })

  it('保存失败时展示错误提示', async () => {
    updateProfile.mockRejectedValue(new Error('手机号格式不正确'))
    const { wrapper } = await mountProfile()
    await setField(wrapper, 'profile-phone', '123')
    await wrapper.get('[data-testid="profile-save"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="profile-message"]').text()).toContain('手机号格式不正确')
  })
})
