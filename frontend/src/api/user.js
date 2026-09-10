// 用户资料接口。契约：docs/api-contract-v1.md §4.3/§4.4（需 CUSTOMER 登录，身份取自 token）。
import { http } from './http'

export function getProfile() {
  return http.get('/users/me')
}

export function updateProfile(payload) {
  return http.patch('/users/me', payload)
}

export function listUsers(params = { page: 1, size: 20 }) {
  return http.get('/admin/users', { params })
}
export function deleteAccount() { return http.delete('/users/me') }
