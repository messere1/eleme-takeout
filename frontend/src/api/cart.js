// 购物车接口。契约：docs/api-contract-v1.md §7（需 CUSTOMER 登录，身份取自 token）。
import { http } from './http'

export function getCart() {
  return http.get('/cart')
}

export function addToCart(payload) {
  return http.post('/cart/items', payload)
}

export function updateItem(itemId, quantity) {
  return http.patch(`/cart/items/${itemId}`, { quantity })
}

export function removeItem(itemId) {
  return http.delete(`/cart/items/${itemId}`)
}

export function clearCart() {
  return http.delete('/cart')
}
export function getDeliveryInfo(shopId) { return http.get(`/cart/delivery-info/${shopId}`) }
export function saveDeliveryInfo(payload) { return http.patch('/cart/delivery-info', payload) }
