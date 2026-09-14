// @vitest-environment jsdom
import { beforeEach, describe, expect, it } from 'vitest'

import router from './index'
import { session } from '@/utils/session'

describe('SRS V1.4 管理员只读页面路由契约', () => {
  beforeEach(async () => {
    session.clear()
    await router.push('/')
  })

  it('提供统一管理员只读页面路由', () => {
    const paths = router.getRoutes().map((route) => route.path)
    expect(paths).toContain('/admin')
  })

  it('新增搜索入口时仍保留骑手与管理员子页面路由', () => {
    const paths = router.getRoutes().map((route) => route.path)
    expect(paths).toContain('/search')
    expect(paths).toContain('/rider')
    expect(paths).toContain('/admin/users')
    expect(paths).toContain('/admin/merchants')
  })

  it('未登录不能通过管理员子页面绕过权限保护', async () => {
    await router.push('/admin/users')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('未登录和非管理员不能进入管理员页面', async () => {
    await router.push('/admin')
    expect(router.currentRoute.value.path).toBe('/login')

    session.save({ token: 'customer-token', role: 'CUSTOMER' })
    await router.push('/admin')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('管理员可以进入只读管理页面', async () => {
    session.save({ token: 'admin-token', role: 'ADMIN' })
    await router.push('/admin')
    expect(router.currentRoute.value.path).toBe('/admin')
  })

  it.each([
    ['CUSTOMER', '/rider'],
    ['MERCHANT', '/admin'],
    ['ADMIN', '/rider'],
    ['RIDER', '/merchant'],
  ])('%s 不能进入其他角色工作区 %s', async (role, targetPath) => {
    session.save({ token: `${role.toLowerCase()}-token`, role })
    await router.push(targetPath)
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it.each([
    ['MERCHANT', '/merchant'],
    ['ADMIN', '/admin'],
    ['RIDER', '/rider'],
  ])('%s 登录后访问顾客首页会回到自己的工作区', async (role, expectedPath) => {
    session.save({ token: `${role.toLowerCase()}-token`, role })
    await router.push('/login')
    await router.push('/')
    expect(router.currentRoute.value.path).toBe(expectedPath)
  })

  it('未登录不能通过详情地址绕过顾客订单保护', async () => {
    await router.push('/orders/60')
    expect(router.currentRoute.value.path).toBe('/login')
  })
})
