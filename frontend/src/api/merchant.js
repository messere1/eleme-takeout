// 商家接口。契约：docs/api-contract-v1.md §4.5 商家注册（201，返回含 shopId）。
import { http } from './http'

export function registerMerchant(payload) {
  return http.post('/merchants', payload)
}
