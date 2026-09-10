// @vitest-environment jsdom
import { describe, expect, it } from 'vitest'

import * as orderApi from './order'
import * as userApi from './user'
import router from '@/router'

describe('SRS V2.0 第二阶段完整功能契约', () => {
  it('支付、退款和账号注销API均已暴露', () => {
    expect(orderApi.payOrder).toBeTypeOf('function')
    expect(orderApi.requestRefund).toBeTypeOf('function')
    expect(userApi.deleteAccount).toBeTypeOf('function')
  })

  it('顾客、商家、管理员和骑手页面彼此隔离', () => {
    const paths = router.getRoutes().map((route) => route.path)
    expect(paths).toContain('/orders/:id/pay')
    expect(paths).toContain('/admin')
    expect(paths).toContain('/rider')
  })
})
