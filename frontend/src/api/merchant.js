// 商家接口。契约：docs/api-contract-v1.md §4.5 商家注册（201，返回含 shopId）。
import { http } from './http'

export function registerMerchant(payload) {
  return http.post('/merchants', payload)
}

export function listMerchants(params = { page: 1, size: 20 }) {
  return http.get('/admin/merchants', { params })
}

export function getMyMerchant() {
  return http.get('/merchants/me')
}
export function listBusinessCategories() { return http.get('/business-categories') }
export function updateMyMerchant(payload) { return http.patch('/merchants/me', payload) }
export function deleteMyMerchant() { return http.delete('/merchants/me') }
export function getMyShopBusinessCategories() { return http.get('/merchant/shop/business-categories') }
export function updateMyShopBusinessCategories(categoryIds) { return http.put('/merchant/shop/business-categories', { categoryIds }) }
