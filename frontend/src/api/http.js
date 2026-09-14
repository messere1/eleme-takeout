// 契约：docs/api-contract-v1.md §2/§9 —— 成功 code===0；错误码为字符串；
// 校验失败 data.fieldErrors；401 AUTH_INVALID/AUTH_EXPIRED 需清理登录态并跳登录。
import axios from 'axios'
import { session } from '@/utils/session'

export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
})

// 登录/注册等公开接口自身的错误要在页面上展示，不应触发全局跳登录。
const PUBLIC_PATHS = ['/auth/login', '/users', '/merchants', '/riders']

// 统一业务错误体：页面只读 message/code/data.fieldErrors。
export class ApiError extends Error {
  constructor(message, { code, httpStatus, data, traceId } = {}) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.httpStatus = httpStatus
    this.data = data
    this.traceId = traceId
  }
}

function isPublicRequest(url = '') {
  return PUBLIC_PATHS.some((path) => url.includes(path))
}

async function handleUnauthorized() {
  session.clear()
  const { default: router } = await import('@/router')
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

// 登录/注册等公开请求不携带令牌（避免残留的过期 token 在公开接口上触发 401）。
const PUBLIC_POST = { '/auth/login': 1, '/users': 1, '/merchants': 1 }

http.interceptors.request.use((config) => {
  const auth = session.load()
  const url = (config.url || '').split('?')[0]
  const method = (config.method || 'get').toLowerCase()
  const isPublic = method === 'post' && PUBLIC_POST[url]
  if (auth?.token && !isPublic) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    // 非统一包装的响应（如普通文件/无结构体）原样透传
    if (!body || typeof body !== 'object' || !('code' in body)) {
      return body
    }
    if (body.code === 0) {
      return body.data
    }
    throw new ApiError(body.msg || '请求失败', {
      code: body.code,
      httpStatus: response.status,
      data: body.data,
      traceId: body.traceId,
    })
  },
  async (error) => {
    const body = error.response?.data
    const status = error.response?.status
    const url = error.config?.url || ''

    // 后端统一包装的错误（含 Spring 校验失败、业务冲突、401/403）
    if (body && typeof body === 'object' && body.code) {
      const apiError = new ApiError(body.msg || '请求失败', {
        code: body.code,
        httpStatus: status,
        data: body.data,
        traceId: body.traceId,
      })
      // 仅当“带了 token 却仍 401/过期”时才清登录态并跳登录；
      // 未登录浏览公开页遇到 401 不跳转，只抛错给页面自行兜底。
      const sentToken = Boolean(error.config?.headers?.Authorization)
      if (
        status === 401 &&
        (body.code === 'AUTH_INVALID' || body.code === 'AUTH_EXPIRED') &&
        sentToken &&
        !isPublicRequest(url)
      ) {
        await handleUnauthorized()
      }
      throw apiError
    }

    // 非统一包装：网络错误 / 后端 500 默认错误体
    const fallbackMsg = body?.message || (status ? `请求失败（HTTP ${status}）` : '网络异常，请稍后重试')
    throw new ApiError(fallbackMsg, { code: 'NETWORK_ERROR', httpStatus: status, data: body })
  },
)

export default http
