// 契约：docs/api-contract-v1.md §4.1（登录）、§4.2（用户注册）。
// http 拦截器负责解包 code===0 并抛出带 message/code/fieldErrors 的 ApiError。
import { http } from './http'

export function login(payload) {
  return http.post('/auth/login', payload)
}

export function register(payload) {
  return http.post('/users', payload)
}
