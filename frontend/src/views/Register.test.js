// @vitest-environment jsdom
// FR-001 注册页 · 红灯基线。
// mock 掉 api 模块；页面须提供 data-testid：register-username/register-phone/
// register-password/register-submit/register-error（见 frontend/TESTING.md）。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/auth', () => ({ login: vi.fn(), register: vi.fn() }))

import { register } from '@/api/auth'
import { mountView } from '@/test/mountView'
import Register from './Register.vue'

const VALID = { username: 'beiyang_user', phone: '13800138000', password: 'abc123' }

async function mountRegister() {
  return mountView(Register, { path: '/register' })
}

async function setField(wrapper, testid, value) {
  const control = wrapper.get(`[data-testid="${testid}"]`)
  const input = control.element.tagName === 'INPUT' ? control : control.find('input')
  await input.setValue(value)
}

async function fillAndSubmit(wrapper, values) {
  for (const [key, value] of Object.entries(values)) {
    await setField(wrapper, `register-${key}`, value)
  }
  await wrapper.get('[data-testid="register-submit"]').trigger('click')
  await flushPromises()
}

describe('FR-001 注册页', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('渲染用户名、手机号、密码输入框与「注册」按钮', async () => {
    const { wrapper } = await mountRegister()
    for (const testid of [
      'register-username',
      'register-phone',
      'register-password',
      'register-submit',
    ]) {
      expect(wrapper.get(`[data-testid="${testid}"]`).exists()).toBe(true)
    }
  })

  it('字段格式不合法时前端提示且不调用注册接口', async () => {
    const { wrapper } = await mountRegister()
    await fillAndSubmit(wrapper, { username: 'ab', phone: '123', password: '123456' })
    expect(register).not.toHaveBeenCalled()
    const text = wrapper.text()
    expect(text).toContain('用户名需 3~30 个字符')
    expect(text).toContain('手机号格式不正确')
    expect(text).toContain('密码需 6~64 位且包含字母和数字')
  })

  it('合法信息提交时以用户名/手机号/密码调用注册接口', async () => {
    register.mockResolvedValue({ id: 1, ...VALID })
    const { wrapper } = await mountRegister()
    await fillAndSubmit(wrapper, VALID)
    expect(register).toHaveBeenCalledTimes(1)
    expect(register).toHaveBeenCalledWith(VALID)
  })

  it('注册成功后跳转到登录页', async () => {
    register.mockResolvedValue({ id: 1, ...VALID })
    const { wrapper, router } = await mountRegister()
    await fillAndSubmit(wrapper, VALID)
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('重复注册（USER_ALREADY_EXISTS）时展示字段级错误', async () => {
    register.mockRejectedValue(
      Object.assign(new Error('用户名或手机号已被注册'), {
        code: 'USER_ALREADY_EXISTS',
        data: { fieldErrors: { username: '该用户名已存在' } },
      }),
    )
    const { wrapper } = await mountRegister()
    await fillAndSubmit(wrapper, VALID)
    expect(wrapper.text()).toContain('该用户名已存在')
  })
})
