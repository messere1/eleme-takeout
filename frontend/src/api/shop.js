// 店铺/分类/商品浏览接口（桩）。方法签名见 docs/api-contract-v1.md §5/§6，
// 页面测试通过 vi.mock('@/api/shop') 在 api 层伪造响应。
export function getShop(_shopId) {
  throw new Error('待功能开发：店铺接口尚未实现')
}

export function listCategories(_shopId) {
  throw new Error('待功能开发：分类接口尚未实现')
}

export function listProducts(_shopId, _categoryId) {
  throw new Error('待功能开发：商品接口尚未实现')
}
