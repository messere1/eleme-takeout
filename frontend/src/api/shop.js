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
