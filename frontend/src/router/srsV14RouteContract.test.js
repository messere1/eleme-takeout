import { describe, expect, it } from 'vitest'

import router from './index'

describe('SRS V1.4 管理员只读页面路由契约', () => {
  it('提供用户和商家账户只读列表路由', () => {
    const paths = router.getRoutes().map((route) => route.path)

    expect(paths).toEqual(expect.arrayContaining([
      '/admin/users',
      '/admin/merchants',
    ]))
  })
})
