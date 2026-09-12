// 店铺/分类/商品浏览接口。契约：docs/api-contract-v1.md §5/§6。
// 公开接口无需登录；响应经 http 拦截器解包 code===0 返回业务 data。
import { http } from './http'

// 店铺列表（公开分页）。
export function listShops(params = { page: 1, size: 20 }) {
  return http.get('/shops', { params })
}

// 店铺搜索：keyword 匹配 店铺名 / 经营类别 / 菜品名。
// 契约（后端待提供）：GET /api/v1/shops/search?keyword= → 分页结构 { items, page, size, total, totalPages }
export function searchShops(keyword, params = { page: 1, size: 20 }) {
  return http.get('/shops/search', { params: { keyword, ...params } })
}

export function getShop(shopId) {
  return http.get(`/shops/${shopId}`)
}

export function listCategories(shopId) {
  return http.get(`/shops/${shopId}/categories`)
}

// 商品按分类分页（SRS V1.2：GET /categories/{id}/products）。
// 兼容旧调用 listProducts(shopId, categoryId) 与新契约 listProducts(categoryId, {page,size})。
export function listProducts(first, second) {
  const isNew = typeof second === 'object'
  const categoryId = isNew ? first : second
  const params = isNew ? second : { page: 1, size: 20 }
  return http.get(`/categories/${categoryId}/products`, { params })
}

// 商品详情（公开）。
export function getProduct(productId) {
  return http.get(`/products/${productId}`)
}

// 商品价格修改（独立接口）。
export function updateProductPrice(productId, price) {
  return http.patch(`/products/${productId}/price`, { price })
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

export function updateCategory(categoryId, payload) {
  return http.patch(`/categories/${categoryId}`, payload)
}

export function deleteCategory(categoryId) {
  return http.delete(`/categories/${categoryId}`)
}

// —— 商家商品管理（契约 §6；写操作接口已存在）——
export function createProduct(shopId, payload) {
  return http.post(`/shops/${shopId}/products`, payload)
}

export function updateProduct(productId, payload) {
  return http.patch(`/products/${productId}`, payload)
}

export function changeProductStatus(productId, status) {
  return http.patch(`/products/${productId}/status`, { status })
}

export function updateProductStock(productId, stock) {
  return http.patch(`/products/${productId}/stock`, { stock })
}

export function deleteProduct(productId) {
  return http.delete(`/products/${productId}`)
}

export function getMyShop() {
  return http.get('/merchant/shop')
}

// 商家视角商品列表（含下架/删除商品）。
export function listMerchantProducts() {
  return http.get('/merchant/products')
}
