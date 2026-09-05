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
export function updateShop(_shopId, _payload) {
  throw new Error('待功能开发：店铺资料修改尚未实现')
}

export function changeStatus(_shopId, _status) {
  throw new Error('待功能开发：营业状态修改尚未实现')
}

export function createCategory(_shopId, _payload) {
  throw new Error('待功能开发：分类新增尚未实现')
}

export function deleteCategory(_categoryId) {
  throw new Error('待功能开发：分类删除尚未实现')
}
