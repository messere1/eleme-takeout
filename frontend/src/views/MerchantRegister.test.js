// @vitest-environment jsdom
// 商家注册页（FR-004）红灯基线。mock 掉 api 模块；页面须提供 data-testid：
// mr-name / mr-phone / mr-password / mr-scope / mr-submit / mr-message。
import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/merchant', () => ({ registerMerchant: vi.fn() }))

import { registerMerchant } from '@/api/merchant'
import { session } from '@/utils/session'
import { mountView } from '@/test/mountView'
import MerchantRegister from './MerchantRegister.vue'

const VALID = {
  merchantName: '北洋餐厅',
  phone: '13900139000',
  password: 'abc123',
  businessScope: '中式快餐',
}
const REGISTERED = { id: 12, merchantName: '北洋餐厅', phone: '13900139000', businessScope: '中式快餐', shopId: 7, shopStatus: 'CLOSED' }

async function mountRegister() {
  return mountView(MerchantRegister, { path: '/merchant/register' })
}

async function setField(wrapper, testid, value) {
  const control = wrapper.get(`[data-testid="${testid}"]`)
  const input = control.element.tagName === 'INPUT' ? control : control.find('input')
  await input.setValue(value)
}

async function fillAndSubmit(wrapper, values) {
  const FIELD = { merchantName: 'name', businessScope: 'scope' }
  for (const [key, value] of Object.entries(values)) {
    await setField(wrapper, `mr-${FIELD[key] || key}`, value)
  }
  await wrapper.get('[data-testid="mr-submit"]').trigger('click')
  await flushPromises()
}

describe('商家注册页', () => {
  beforeEach(() => {
    session.clear()
    vi.clearAllMocks()
  })

  it('渲染商家名称、手机号、密码、经营范围输入框与提交按钮', async () => {
    const { wrapper } = await mountRegister()
    for (const testid of ['mr-name', 'mr-phone', 'mr-password', 'mr-scope', 'mr-submit']) {
      expect(wrapper.get(`[data-testid="${testid}"]`).exists()).toBe(true)
    }
  })

  it('手机号不合法时不调用接口并提示', async () => {
    const { wrapper } = await mountRegister()
    await fillAndSubmit(wrapper, { ...VALID, phone: '123' })
    expect(registerMerchant).not.toHaveBeenCalled()
    expect(wrapper.get('[data-testid="mr-message"]').text()).toContain('手机号格式不正确')
  })

  it('合法信息提交时调用注册接口并缓存我的店铺、跳转登录', async () => {
    registerMerchant.mockResolvedValue(REGISTERED)
    const { wrapper, router } = await mountRegister()
    await fillAndSubmit(wrapper, VALID)
    expect(registerMerchant).toHaveBeenCalledWith(VALID)
    expect(session.loadShop()?.shopId).toBe(7)
    expect(router.currentRoute.value.path).toBe('/merchant/login')
  })

  it('商家已存在（409）时展示提示', async () => {
    registerMerchant.mockRejectedValue(new Error('商家名称或手机号已存在'))
    const { wrapper } = await mountRegister()
    await fillAndSubmit(wrapper, VALID)
    expect(wrapper.get('[data-testid="mr-message"]').text()).toContain('商家名称或手机号已存在')
  })
})
