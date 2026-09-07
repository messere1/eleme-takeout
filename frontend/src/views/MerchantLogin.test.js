// @vitest-environment jsdom
// 商家登录页红灯基线。mock 掉 api 模块；页面须提供 data-testid：
// merchant-login-phone / merchant-login-password / merchant-login-submit /
// merchant-login-error / merchant-login-to-register。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/auth', () => ({ login: vi.fn(), register: vi.fn() }))

import { login } from '@/api/auth'
import { session } from '@/utils/session'
import { mountView } from '@/test/mountView'
import MerchantLogin from './MerchantLogin.vue'

const PHONE = '13900139000'
const PASSWORD = 'abc123'

async function mountMerchantLogin() {
  return mountView(MerchantLogin, { path: '/merchant/login' })
}

async function setField(wrapper, testid, value) {
  const control = wrapper.get(`[data-testid="${testid}"]`)
  const input = control.element.tagName === 'INPUT' ? control : control.find('input')
  await input.setValue(value)
}

async function fillAndSubmit(wrapper) {
  await setField(wrapper, 'merchant-login-phone', PHONE)
  await setField(wrapper, 'merchant-login-password', PASSWORD)
  await wrapper.get('[data-testid="merchant-login-submit"]').trigger('click')
  await flushPromises()
}

describe('商家登录页', () => {
  beforeEach(() => {
    session.clear()
    vi.clearAllMocks()
  })

  it('渲染手机号、密码、提交按钮与入驻链接', async () => {
    const { wrapper } = await mountMerchantLogin()
    expect(wrapper.get('[data-testid="merchant-login-phone"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="merchant-login-password"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="merchant-login-submit"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="merchant-login-to-register"]').exists()).toBe(true)
  })

  it('提交时以 MERCHANT 角色调用登录并跳转商家后台', async () => {
    login.mockResolvedValue({ token: 'mt-1', role: 'MERCHANT', expiresIn: 7200 })
    const { wrapper, router } = await mountMerchantLogin()
    await fillAndSubmit(wrapper)
    expect(login).toHaveBeenCalledWith({ account: PHONE, password: PASSWORD, role: 'MERCHANT' })
    expect(session.load()?.role).toBe('MERCHANT')
    expect(router.currentRoute.value.path).toBe('/merchant')
  })

  it('空表单提交时提示且不调用登录', async () => {
    const { wrapper } = await mountMerchantLogin()
    await wrapper.get('[data-testid="merchant-login-submit"]').trigger('click')
    await flushPromises()
    expect(login).not.toHaveBeenCalled()
    expect(wrapper.get('[data-testid="merchant-login-error"]').text()).toContain('请输入')
  })

  it('登录失败时展示错误信息', async () => {
    login.mockRejectedValue(new Error('账号或密码错误'))
    const { wrapper } = await mountMerchantLogin()
    await fillAndSubmit(wrapper)
    expect(wrapper.get('[data-testid="merchant-login-error"]').text()).toContain('账号或密码错误')
  })
})
