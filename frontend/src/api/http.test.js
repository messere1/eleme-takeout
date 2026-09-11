// @vitest-environment jsdom
// http 适配层单测：统一解包 code===0、自动携带 Bearer、401 清理登录态跳转登录、
// 业务/字段错误抛 ApiError。通过临时替换 axios adapter 模拟后端响应，不依赖真实后端。
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/router', () => ({
  default: { currentRoute: { value: { path: '/current' } }, push: vi.fn() },
}))

import { ApiError, http } from './http'
import router from '@/router'
import { session } from '@/utils/session'

function respond(status, data) {
  http.defaults.adapter = async (config) => {
    const response = { data, status, statusText: 'OK', headers: {}, config }
    if (status >= 200 && status < 300) return response
    const error = new Error(`Request failed with status code ${status}`)
    error.response = response
    error.config = config
    throw error
  }
}

beforeEach(() => {
  session.clear()
  vi.clearAllMocks()
})

afterEach(() => {
  delete http.defaults.adapter
})

describe('http 适配层', () => {
  it('业务成功(code===0)时解包并直接返回 data', async () => {
    respond(200, {
      code: 0,
      msg: 'success',
      data: { token: 't-1', role: 'CUSTOMER', expiresIn: 7200 },
      traceId: 'x',
    })
    const data = await http.post('/auth/login', {})
    expect(data).toEqual({ token: 't-1', role: 'CUSTOMER', expiresIn: 7200 })
  })

  it('已登录时自动携带 Authorization: Bearer <token>', async () => {
    session.save({ token: 'tok-1', role: 'CUSTOMER' })
    let sent
    http.defaults.adapter = async (config) => {
      sent = config
      return {
        data: { code: 0, msg: 'success', data: null, traceId: 'x' },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      }
    }
    await http.get('/cart')
    expect(sent.headers?.Authorization).toBe('Bearer tok-1')
  })

  it('字段校验失败(400 VALIDATION_ERROR)抛带 fieldErrors 的 ApiError', async () => {
    respond(400, {
      code: 'VALIDATION_ERROR',
      msg: '请求参数校验失败',
      data: { fieldErrors: { username: '用户名已存在' } },
      traceId: 'x',
    })
    const error = await http.post('/users', {}).catch((e) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error.code).toBe('VALIDATION_ERROR')
    expect(error.data.fieldErrors.username).toBe('用户名已存在')
    expect(error.traceId).toBe('x')
  })

  it('受保护接口 401/令牌过期时清理登录态并跳转登录页', async () => {
    session.save({ token: 'expired', role: 'CUSTOMER' })
    respond(401, {
      code: 'AUTH_EXPIRED',
      msg: '登录已过期，请重新登录',
      data: null,
      traceId: 'x',
    })
    await expect(http.get('/cart')).rejects.toMatchObject({ code: 'AUTH_EXPIRED' })
    expect(session.load()).toBeNull()
    expect(router.push).toHaveBeenCalledWith('/login')
  })

  it('登录接口自身的 401（密码错误）不跳转，仅抛出错误供页面展示', async () => {
    respond(401, {
      code: 'AUTH_INVALID',
      msg: '用户名或密码错误',
      data: null,
      traceId: 'x',
    })
    await expect(http.post('/auth/login', {})).rejects.toMatchObject({
      code: 'AUTH_INVALID',
    })
    expect(router.push).not.toHaveBeenCalled()
  })

  it.each([
    [403, 'FORBIDDEN'],
    [404, 'RESOURCE_NOT_FOUND'],
    [409, 'BUSINESS_CONFLICT'],
    [500, 'INTERNAL_ERROR'],
  ])('保留 HTTP %i 失败响应的业务码和 traceId', async (status, code) => {
    respond(status, {
      code,
      msg: '可理解的错误提示',
      data: null,
      traceId: `trace-${status}`,
    })

    await expect(http.get('/protected')).rejects.toMatchObject({
      code,
      httpStatus: status,
      traceId: `trace-${status}`,
      message: '可理解的错误提示',
    })
  })

  it('公开注册请求不携带残留令牌', async () => {
    session.save({ token: 'stale-token', role: 'CUSTOMER' })
    let sent
    http.defaults.adapter = async (config) => {
      sent = config
      return {
        data: { code: 0, msg: 'success', data: {}, traceId: 'x' },
        status: 201,
        statusText: 'Created',
        headers: {},
        config,
      }
    }

    await http.post('/users', {})
    expect(sent.headers?.Authorization).toBeUndefined()
  })
})
