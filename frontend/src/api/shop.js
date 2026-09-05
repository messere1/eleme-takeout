// 店铺/分类/商品浏览接口。契约：docs/api-contract-v1.md §5/§6。
// 公开接口无需登录；响应经 http 拦截器解包 code===0 返回业务 data。
import { http } from './http'

export function getShop(shopId) {
  return http.get(`/shops/${shopId}`)
}

export function listCategories(shopId) {
  return http.get(`/shops/${shopId}/categories`)
}

export function listProducts(shopId, categoryId) {
  return http.get(`/shops/${shopId}/products`, { params: { categoryId } })
}

// —— 商家后台写操作（需 MERCHANT token，仅能操作本人店铺；契约 §5/§6）——
export function updateShop(shopId, payload) {
  return http.patch(`/shops/${shopId}`, payload)
}

export function changeStatus(shopId, status) {
  return http.patch(`/shops/${shopId}/status`, { status })
}

export function createCategory(shopId, payload) {
  return http.post(`/shops/${shopId}/categories`, payload)
}

export function deleteCategory(categoryId) {
  return http.delete(`/categories/${categoryId}`)
}
