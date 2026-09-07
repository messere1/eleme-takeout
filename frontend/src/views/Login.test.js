// @vitest-environment jsdom
// FR-002 登录页 · 红灯基线。
// mock 掉 api 模块；页面须提供 data-testid：login-account/login-password/
// login-submit/login-error（见 frontend/TESTING.md）。跳转断言读 router.currentRoute。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/auth', () => ({ login: vi.fn(), register: vi.fn() }))

import { login } from '@/api/auth'
import { session } from '@/utils/session'
import { mountView } from '@/test/mountView'
import Login from './Login.vue'

const ACCOUNT = '13800138000'
const PASSWORD = 'abc123'
const TOKEN = { token: 'tok-1', role: 'CUSTOMER', expiresIn: 7200 }

async function mountLogin() {
  return mountView(Login, { path: '/login' })
}

async function setField(wrapper, testid, value) {
  const control = wrapper.get(`[data-testid="${testid}"]`)
  const input = control.element.tagName === 'INPUT' ? control : control.find('input')
  await input.setValue(value)
}

async function clickSubmit(wrapper) {
  await wrapper.get('[data-testid="login-submit"]').trigger('click')
}

describe('FR-002 登录页', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('渲染账号、密码输入框与「登录」按钮', async () => {
    const { wrapper } = await mountLogin()
    expect(wrapper.get('[data-testid="login-account"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="login-password"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="login-submit"]').exists()).toBe(true)
  })

  it('空表单提交时提示必填且不调用登录接口', async () => {
    const { wrapper } = await mountLogin()
    await clickSubmit(wrapper)
    await flushPromises()
    expect(login).not.toHaveBeenCalled()
    expect(wrapper.get('[data-testid="login-error"]').text()).toContain('请输入')
  })

  it('合法账号密码提交时以 CUSTOMER 角色调用登录接口', async () => {
    login.mockResolvedValue(TOKEN)
    const { wrapper } = await mountLogin()
    await setField(wrapper, 'login-account', ACCOUNT)
    await setField(wrapper, 'login-password', PASSWORD)
    await clickSubmit(wrapper)
    await flushPromises()
    expect(login).toHaveBeenCalledTimes(1)
    expect(login).toHaveBeenCalledWith({ account: ACCOUNT, password: PASSWORD, role: 'CUSTOMER' })
  })

  it('登录成功时保存 token/角色并跳转到首页', async () => {
    login.mockResolvedValue(TOKEN)
    const { wrapper, router } = await mountLogin()
    await setField(wrapper, 'login-account', ACCOUNT)
    await setField(wrapper, 'login-password', PASSWORD)
    await clickSubmit(wrapper)
    await flushPromises()
    expect(session.load()?.token).toBe(TOKEN.token)
    expect(session.load()?.role).toBe(TOKEN.role)
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('登录失败时展示后端错误信息', async () => {
    login.mockRejectedValue(
      Object.assign(new Error('用户名或密码错误'), { code: 'AUTH_INVALID' }),
    )
    const { wrapper } = await mountLogin()
    await setField(wrapper, 'login-account', ACCOUNT)
    await setField(wrapper, 'login-password', PASSWORD)
    await clickSubmit(wrapper)
    await flushPromises()
    expect(wrapper.get('[data-testid="login-error"]').text()).toContain('用户名或密码错误')
  })

  it('请求挂起期间提交按钮被禁用，防止重复提交', async () => {
    let release
    login.mockReturnValue(new Promise((resolve) => { release = resolve }))
    const { wrapper } = await mountLogin()
    await setField(wrapper, 'login-account', ACCOUNT)
    await setField(wrapper, 'login-password', PASSWORD)
    await clickSubmit(wrapper)
    expect(wrapper.get('[data-testid="login-submit"]').element.disabled).toBe(true)
    release(TOKEN)
    await flushPromises()
  })

  it('渲染登录角色选择控件（默认顾客）', async () => {
    const { wrapper } = await mountLogin()
    expect(wrapper.get('[data-testid="login-role"]').element.value).toBe('CUSTOMER')
  })

  it('选择商家角色登录后进入商家后台', async () => {
    login.mockResolvedValue({ token: 'mt-1', role: 'MERCHANT', expiresIn: 7200 })
    const { wrapper, router } = await mountLogin()
    await wrapper.get('[data-testid="login-role"]').setValue('MERCHANT')
    await setField(wrapper, 'login-account', ACCOUNT)
    await setField(wrapper, 'login-password', PASSWORD)
    await clickSubmit(wrapper)
    await flushPromises()
    expect(login).toHaveBeenCalledWith({ account: ACCOUNT, password: PASSWORD, role: 'MERCHANT' })
    expect(session.load()?.role).toBe('MERCHANT')
    expect(router.currentRoute.value.path).toBe('/merchant')
  })
})
