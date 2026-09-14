// @vitest-environment jsdom
// 注册页（顾客/商家统一角色化 + 注册后自动登录）红灯基线。
// 页面须提供 data-testid：register-role / register-username / register-merchant-name /
// register-phone / register-password / register-scope / register-submit / register-error。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/auth', () => ({ register: vi.fn(), login: vi.fn() }))
vi.mock('@/api/merchant', () => ({ registerMerchant: vi.fn(), listBusinessCategories: vi.fn().mockResolvedValue([]) }))
vi.mock('@/api/rider', () => ({ registerRider: vi.fn() }))

import { login, register } from '@/api/auth'
import { registerMerchant } from '@/api/merchant'
import { registerRider } from '@/api/rider'
import { session } from '@/utils/session'
import { mountView } from '@/test/mountView'
import Register from './Register.vue'

const CUSTOMER = { username: 'beiyang_user', phone: '13800138000', password: 'abc12345' }
const MERCHANT = { merchantName: '北洋餐厅', phone: '13900139000', password: 'abc12345', businessScope: '中式快餐', shopAddress: '天津大学北洋园校区' }
const RIDER = { riderName: '骑手小王', phone: '13700137000', password: 'abc12345' }

async function mountRegister() {
  return mountView(Register, { path: '/register' })
}

async function setField(wrapper, testid, value) {
  const control = wrapper.get(`[data-testid="${testid}"]`)
  const input = control.element.tagName === 'INPUT' ? control : control.find('input')
  await input.setValue(value)
}

async function submit(wrapper) {
  await wrapper.get('[data-testid="register-submit"]').trigger('click')
  await flushPromises()
}

describe('注册页（角色化）', () => {
  beforeEach(() => {
    session.clear()
    vi.clearAllMocks()
  })

  it('默认顾客角色并渲染顾客字段与角色选择', async () => {
    const { wrapper } = await mountRegister()
    expect(wrapper.get('[data-testid="register-role"]').element.value).toBe('CUSTOMER')
    expect(wrapper.get('[data-testid="register-username"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="register-phone"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="register-password"]').exists()).toBe(true)
  })

  it('切换商家角色后展示商家专属字段', async () => {
    const { wrapper } = await mountRegister()
    await wrapper.get('[data-testid="register-role"]').setValue('MERCHANT')
    expect(wrapper.get('[data-testid="register-merchant-name"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="register-scope"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="register-username"]').exists()).toBe(false)
  })

  it('顾客注册成功自动登录并回到首页', async () => {
    register.mockResolvedValue({ id: 1, username: CUSTOMER.username, phone: CUSTOMER.phone })
    login.mockResolvedValue({ token: 'cu-1', role: 'CUSTOMER', expiresIn: 7200 })
    const { wrapper, router } = await mountRegister()
    for (const [key, value] of Object.entries(CUSTOMER)) {
      await setField(wrapper, `register-${key}`, value)
    }
    await submit(wrapper)
    expect(register).toHaveBeenCalledWith(CUSTOMER)
    expect(login).toHaveBeenCalledWith({
      account: CUSTOMER.phone,
      password: CUSTOMER.password,
      role: 'CUSTOMER',
    })
    expect(session.load()?.token).toBe('cu-1')
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('商家注册成功自动登录并进入商家后台', async () => {
    registerMerchant.mockResolvedValue({ id: 12, merchantName: '北洋餐厅', phone: '13900139000', businessScope: '中式快餐', shopId: 7, shopStatus: 'CLOSED' })
    login.mockResolvedValue({ token: 'mt-1', role: 'MERCHANT', expiresIn: 7200 })
    const { wrapper, router } = await mountRegister()
    await wrapper.get('[data-testid="register-role"]').setValue('MERCHANT')
    await setField(wrapper, 'register-merchant-name', MERCHANT.merchantName)
    await setField(wrapper, 'register-phone', MERCHANT.phone)
    await setField(wrapper, 'register-password', MERCHANT.password)
    await setField(wrapper, 'register-scope', MERCHANT.businessScope)
    await setField(wrapper, 'register-shop-address', MERCHANT.shopAddress)
    await submit(wrapper)
    expect(registerMerchant).toHaveBeenCalledWith(MERCHANT)
    expect(login).toHaveBeenCalledWith({
      account: MERCHANT.phone,
      password: MERCHANT.password,
      role: 'MERCHANT',
    })
    expect(session.loadShop()?.shopId).toBe(7)
    expect(session.load()?.role).toBe('MERCHANT')
    expect(router.currentRoute.value.path).toBe('/merchant')
  })

  it('骑手注册成功自动登录并进入配送台', async () => {
    registerRider.mockResolvedValue({ id: 21, riderName: RIDER.riderName, phone: RIDER.phone })
    login.mockResolvedValue({ token: 'rd-1', role: 'RIDER', expiresIn: 7200 })
    const { wrapper, router } = await mountRegister()
    await wrapper.get('[data-testid="register-role"]').setValue('RIDER')
    await setField(wrapper, 'register-rider-name', RIDER.riderName)
    await setField(wrapper, 'register-phone', RIDER.phone)
    await setField(wrapper, 'register-password', RIDER.password)
    await submit(wrapper)
    expect(registerRider).toHaveBeenCalledWith(RIDER)
    expect(login).toHaveBeenCalledWith({ account: RIDER.phone, password: RIDER.password, role: 'RIDER' })
    expect(session.load()?.role).toBe('RIDER')
    expect(router.currentRoute.value.path).toBe('/rider')
  })

  it('顾客手机号格式不合法时不调用注册接口并提示', async () => {
    const { wrapper } = await mountRegister()
    await setField(wrapper, 'register-username', CUSTOMER.username)
    await setField(wrapper, 'register-phone', '123')
    await setField(wrapper, 'register-password', CUSTOMER.password)
    await submit(wrapper)
    expect(register).not.toHaveBeenCalled()
    expect(wrapper.get('[data-testid="register-error"]').text()).toContain('手机号格式不正确')
  })

  it('注册请求未完成时禁用提交按钮防止重复提交', async () => {
    register.mockReturnValue(new Promise(() => {}))
    const { wrapper } = await mountRegister()
    for (const [key, value] of Object.entries(CUSTOMER)) {
      await setField(wrapper, `register-${key}`, value)
    }

    await wrapper.get('[data-testid="register-submit"]').trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.get('[data-testid="register-submit"]').element.disabled).toBe(true)
    expect(register).toHaveBeenCalledTimes(1)
  })
})
