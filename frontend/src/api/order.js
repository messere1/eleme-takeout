// 订单接口（顾客下单/列表/详情，商家接单/完成，顾客取消）。契约 §8 与商家扩展动作。
import { http } from './http'

export function createOrder() {
  return http.post('/orders')
}

export function listOrders(query) {
  return http.get('/orders', { params: query })
}

export function getOrder(orderId) {
  return http.get(`/orders/${orderId}`)
}

export function listMerchantOrders() {
  return http.get('/merchant/orders')
}

export function acceptOrder(orderId) {
  return http.post(`/orders/${orderId}/accept`)
}

export function completeOrder(orderId) {
  return http.post(`/orders/${orderId}/complete`)
}

export function cancelOrder(orderId) {
  return http.post(`/orders/${orderId}/cancel`)
}

export function confirmOrder(orderId) {
  return http.post(`/orders/${orderId}/confirm`)
}
